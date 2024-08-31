package com.example.pocketledger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener, ExpenseAdapter.OnExpenseDeleteListener {

    private static final String TAG = "MainActivity";
    private RecyclerView expensesRecyclerView;
    private RecyclerView categoryTotalsRecyclerView;
    private Button addCategoryButton;
    private EditText categoryInput;
    private Spinner categorySpinner;
    private ExpenseAdapter expenseAdapter;
    private CategoryTotalAdapter categoryTotalAdapter;
    private List<Expense> expenses = new ArrayList<>();
    private List<CategoryTotal> categoryTotals = new ArrayList<>();
    private List<String> categories = new ArrayList<>();

    private BroadcastReceiver dataClearedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SettingsActivity.ACTION_DATA_CLEARED.equals(intent.getAction())) {
                refreshAllData();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();
        setupAddCategoryButton();
        loadExpenses();
        loadCategoryTotals();
        setupCategorySpinner();
        initializeDefaultCategories();
        checkWearConnection();

        // Register the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(dataClearedReceiver,
                new IntentFilter(SettingsActivity.ACTION_DATA_CLEARED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataClearedReceiver);
    }

    private void refreshAllData() {
        loadExpenses();
        loadCategoryTotals();
        initializeDefaultCategories();
    }

    private void initViews() {
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView);
        categoryTotalsRecyclerView = findViewById(R.id.categoryTotalsRecyclerView);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        categoryInput = findViewById(R.id.categoryInput);
        categorySpinner = findViewById(R.id.categorySpinner);
    }

    private void setupRecyclerViews() {

        expenseAdapter = new ExpenseAdapter(expenses, this);
        expensesRecyclerView.setAdapter(expenseAdapter);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        categoryTotalAdapter = new CategoryTotalAdapter(categoryTotals);
        categoryTotalsRecyclerView.setAdapter(categoryTotalAdapter);
        categoryTotalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAddCategoryButton() {
        addCategoryButton.setOnClickListener(v -> {
            String newCategory = categoryInput.getText().toString().trim();
            if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
                addCategory(newCategory);
                categoryInput.getText().clear();
                Toast.makeText(this, "Category added: " + newCategory, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCategory(String category) {
        new Thread(() -> {
            AppDatabase.getInstance(this).categoryDao().insert(new Category(category));
            runOnUiThread(() -> {
                categories.add(category);
                setupCategorySpinner();
                sendCategoriesToWear();
            });
        }).start();
    }

    private void loadExpenses() {
        new Thread(() -> {
            List<Expense> loadedExpenses = AppDatabase.getInstance(this).expenseDao().getAllExpenses();
            runOnUiThread(() -> {
                expenses.clear();
                expenses.addAll(loadedExpenses);
                expenseAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadCategoryTotals() {
        new Thread(() -> {
            List<CategoryTotal> loadedTotals = AppDatabase.getInstance(this).expenseDao().getExpenseTotalsByCategory();
            runOnUiThread(() -> {
                categoryTotals.clear();
                categoryTotals.addAll(loadedTotals);
                categoryTotalAdapter.notifyDataSetChanged();
            });
        }).start();
    }


    private void initializeDefaultCategories() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<String> existingCategories = db.categoryDao().getAllCategories();
            categories.clear();
            categories.addAll(existingCategories);
            runOnUiThread(this::setupCategorySpinner);
        }).start();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        if (categories.isEmpty()) {
            categorySpinner.setEnabled(false);
            // Optionally, you can set a hint or a placeholder text
            categorySpinner.setPrompt("No categories available");
        } else {
            categorySpinner.setEnabled(true);
        }
    }

    private void sendCategoriesToWear() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<String> updatedCategories = db.categoryDao().getAllCategories();

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/categories");
            putDataMapReq.getDataMap().putStringArray("categories", updatedCategories.toArray(new String[0]));
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();

            Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);

            putDataTask.addOnSuccessListener(dataItem -> {
                Log.d(TAG, "Categories sent successfully to Wear: " + updatedCategories);
            });

            putDataTask.addOnFailureListener(exception -> {
                Log.e(TAG, "Sending categories to Wear failed: " + exception);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals("/categories")) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                    String[] newCategories = dataMapItem.getDataMap().getStringArray("categories");
                    if (newCategories != null) {
                        updateCategories(newCategories);
                    }
                }
            }
        }
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals("/expense")) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    double amount = dataMap.getDouble("amount");
                    String category = dataMap.getString("category");
                    long timestamp = dataMap.getLong("timestamp");
                    Log.d(TAG, "Received expense from Wear: Amount=" + amount + ", Category=" + category + ", Timestamp=" + timestamp);
                    processExpense(amount, category, timestamp);
                }
            }
        }
    }

    private void updateCategories(String[] newCategories) {
        categories.clear();
        categories.addAll(Arrays.asList(newCategories));
        runOnUiThread(this::setupCategorySpinner);
    }

    private void processExpense(double amount, String category, long timestamp) {
        Expense expense = new Expense(amount, category, timestamp);
        new Thread(() -> {
            AppDatabase.getInstance(this).expenseDao().insertExpense(expense);
            runOnUiThread(() -> {
                expenses.add(0, expense);
                expenseAdapter.notifyItemInserted(0);
                expensesRecyclerView.scrollToPosition(0);
                loadCategoryTotals();
                Log.d(TAG, "Expense processed and added to the list: " + expense);
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkWearConnection() {
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    if (!nodes.isEmpty()) {
                        for (Node node : nodes) {
                            Log.d(TAG, "Connected to Wear device: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                        }
                    } else {
                        Log.w(TAG, "No Wear devices connected");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get connected nodes", e));
    }

    @Override
    public void onExpenseDelete(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> deleteExpense(expense))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.expenseDao().deleteExpense(expense);

            // Recalculate category totals
            List<CategoryTotal> updatedTotals = db.expenseDao().getExpenseTotalsByCategory();

            runOnUiThread(() -> {
                int position = expenses.indexOf(expense);
                if (position != -1) {
                    expenses.remove(position);
                    expenseAdapter.notifyItemRemoved(position);

                    // Update category totals
                    categoryTotals.clear();
                    categoryTotals.addAll(updatedTotals);
                    categoryTotalAdapter.notifyDataSetChanged();

                    Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}