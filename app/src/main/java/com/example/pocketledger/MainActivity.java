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

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

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
        expenseAdapter = new ExpenseAdapter(expenses);
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

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void initializeDefaultCategories() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<String> existingCategories = db.categoryDao().getAllCategories();
            if (existingCategories.isEmpty()) {
                String[] defaultCategories = {"Food", "Transportation", "Entertainment", "Utilities"};
                for (String category : defaultCategories) {
                    db.categoryDao().insert(new Category(category));
                }
                Log.d(TAG, "Default categories initialized");
            }
            categories.clear();
            categories.addAll(db.categoryDao().getAllCategories());
            runOnUiThread(this::setupCategorySpinner);
        }).start();
    }

    private void sendCategoriesToWear() {
        // Implementation for sending categories to wear device
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
                if (item.getUri().getPath().equals("/expense")) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                    double amount = dataMapItem.getDataMap().getDouble("amount");
                    String category = dataMapItem.getDataMap().getString("category");
                    long timestamp = dataMapItem.getDataMap().getLong("timestamp");
                    processExpense(amount, category, timestamp);
                }
            }
        }
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
}