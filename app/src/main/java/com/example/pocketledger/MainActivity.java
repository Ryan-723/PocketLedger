package com.example.pocketledger;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

    private static final String TAG = "MainActivity";
    private static final String CATEGORIES_PATH = "/categories";

    private RecyclerView expensesRecyclerView;
    private RecyclerView categoryTotalsRecyclerView;
    private Button addCategoryButton;
    private EditText categoryInput;
    private Spinner categorySpinner;
    private ExpenseAdapter expenseAdapter;
    private CategoryTotalAdapter categoryTotalAdapter;
    private List<Expense> expenses = new ArrayList<>();
    private List<CategoryTotal> categoryTotals = new ArrayList<>();
    private List<String> categories = new ArrayList<>(Arrays.asList("Food", "Transportation", "Entertainment", "Utilities"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Activity created");

        expensesRecyclerView = findViewById(R.id.expensesRecyclerView);
        categoryTotalsRecyclerView = findViewById(R.id.categoryTotalsRecyclerView);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        categoryInput = findViewById(R.id.categoryInput);
        categorySpinner = findViewById(R.id.categorySpinner);

        // Set up RecyclerViews for expenses and category totals
        setupRecyclerViews();
        // Set up the add category button functionality
        setupAddCategoryButton();
        // Load initial data from the database
        loadExpenses();
        loadCategoryTotals();
        // Set up the category spinner
        setupCategorySpinner();
        // Check connection with wearable device
        checkWearableConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering data listener");
        // Register the DataClient listener to receive data from wearable
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregistering data listener");
        // Unregister the DataClient listener
        Wearable.getDataClient(this).removeListener(this);
    }

    private void checkWearableConnection() {
        // Check for connected wearable devices
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    if (!nodes.isEmpty()) {
                        for (Node node : nodes) {
                            Log.d(TAG, "Connected to node: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                        }
                    } else {
                        Log.w(TAG, "No connected nodes found");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get connected nodes", e));
    }

    private void setupRecyclerViews() {
        // Set up RecyclerView for expenses
        Log.d(TAG, "setupRecyclerViews: Setting up RecyclerViews");
        expenseAdapter = new ExpenseAdapter(expenses);
        expensesRecyclerView.setAdapter(expenseAdapter);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up RecyclerView for category totals
        categoryTotalAdapter = new CategoryTotalAdapter(categoryTotals);
        categoryTotalsRecyclerView.setAdapter(categoryTotalAdapter);
        categoryTotalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupCategorySpinner() {
        Log.d(TAG, "setupCategorySpinner: Setting up category spinner");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupAddCategoryButton() {
        Log.d(TAG, "setupAddCategoryButton: Setting up add category button");
        addCategoryButton.setOnClickListener(v -> {
            String newCategory = categoryInput.getText().toString().trim();
            Log.d(TAG, "setupAddCategoryButton: New category input: " + newCategory);
            if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
                categories.add(newCategory);
                categoryInput.getText().clear();
                setupCategorySpinner();
                sendCategoriesToWear();
                Toast.makeText(this, "Category added: " + newCategory, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "setupAddCategoryButton: Category added: " + newCategory);
            } else {
                Log.d(TAG, "setupAddCategoryButton: Invalid or duplicate category");
                Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExpenses() {
        Log.d(TAG, "loadExpenses: Loading expenses from database");
        // Load expenses from the database in a background thread
        new Thread(() -> {
            List<Expense> loadedExpenses = AppDatabase.getInstance(this).expenseDao().getAllExpenses();
            Log.d(TAG, "loadExpenses: Loaded " + loadedExpenses.size() + " expenses");
            runOnUiThread(() -> {
                expenses.clear();
                expenses.addAll(loadedExpenses);
                expenseAdapter.notifyDataSetChanged();
                Log.d(TAG, "loadExpenses: Updated RecyclerView with loaded expenses");
            });
        }).start();
    }

    private void loadCategoryTotals() {
        Log.d(TAG, "loadCategoryTotals: Loading category totals from database");
        // Load category totals from the database in a background thread
        new Thread(() -> {
            List<CategoryTotal> loadedTotals = AppDatabase.getInstance(this).expenseDao().getExpenseTotalsByCategory();
            Log.d(TAG, "loadCategoryTotals: Loaded " + loadedTotals.size() + " category totals");
            runOnUiThread(() -> {
                categoryTotals.clear();
                categoryTotals.addAll(loadedTotals);
                categoryTotalAdapter.notifyDataSetChanged();
                Log.d(TAG, "loadCategoryTotals: Updated RecyclerView with loaded category totals");
            });
        }).start();
    }

    private void sendCategoriesToWear() {
        Log.d(TAG, "sendCategoriesToWear: Preparing to send categories: " + categories);
        // Send a message to the wearable device about updated categories
        sendMessageToWear("Categories updated: " + categories.toString());

        // Prepare data to send to wearable
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(CATEGORIES_PATH);
        putDataMapReq.getDataMap().putStringArray("categories", categories.toArray(new String[0]));
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        // Send data to wearable
        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);

        putDataTask.addOnSuccessListener(dataItem -> {
            Log.d(TAG, "sendCategoriesToWear: Categories sent successfully: " + dataItem.getUri());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "sendCategoriesToWear: Failed to send categories", e);
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: Data changed event received");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                Log.d(TAG, "onDataChanged: Data item changed: " + item.getUri());
                // Process received expense data from wearable
                if (item.getUri().getPath().equals("/expense")) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    double amount = dataMap.getDouble("amount");
                    String category = dataMap.getString("category");
                    long timestamp = dataMap.getLong("timestamp");
                    Log.d(TAG, "onDataChanged: Received expense - Amount: " + amount + ", Category: " + category + ", Timestamp: " + timestamp);
                    processExpense(amount, category, timestamp);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "onDataChanged: Data item deleted: " + event.getDataItem().getUri());
            }
        }
    }

    private void processExpense(double amount, String category, long timestamp) {
        Log.d(TAG, "processExpense: Processing new expense");
        Expense expense = new Expense(amount, category, timestamp);
        // Insert the new expense into the database in a background thread
        new Thread(() -> {
            AppDatabase.getInstance(this).expenseDao().insertExpense(expense);
            Log.d(TAG, "processExpense: Expense inserted into database");
            runOnUiThread(() -> {
                expenses.add(0, expense);
                expenseAdapter.notifyItemInserted(0);
                expensesRecyclerView.scrollToPosition(0);
                Log.d(TAG, "processExpense: RecyclerView updated with new expense");
                loadCategoryTotals();
            });
        }).start();
    }

    private void sendMessageToWear(final String message) {
        Log.d(TAG, "sendMessageToWear: Attempting to send message: " + message);
        // Send a message to all connected wearable devices
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    for (Node node : nodes) {
                        Wearable.getMessageClient(this).sendMessage(node.getId(), "/message", message.getBytes())
                                .addOnSuccessListener(messageId -> Log.d(TAG, "Message sent successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to send message", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get connected nodes", e));
    }
}