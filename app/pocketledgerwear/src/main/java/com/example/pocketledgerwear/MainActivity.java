package com.example.pocketledgerwear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends FragmentActivity implements DataClient.OnDataChangedListener {

    private static final String TAG = "WearMainActivity";
    private EditText amountInput;
    private Spinner categorySpinner;
    private Button saveButton;
    private List<String> categories = new ArrayList<>(Arrays.asList("Food", "Transportation", "Entertainment", "Utilities"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter("com.example.pocketledgerwear.UPDATE_CATEGORIES");
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter);

        amountInput = findViewById(R.id.amountInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);

        setupCategorySpinner();
        setupSaveButton();
        requestCategoriesFromPhone();
        checkPhoneConnection();
    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.pocketledgerwear.UPDATE_CATEGORIES".equals(intent.getAction())) {
                loadCategories();
            }
        }
    };

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
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }

    private void loadCategories() {
        SharedPreferences prefs = getSharedPreferences("CategoryPrefs", MODE_PRIVATE);
        Set<String> categoriesSet = prefs.getStringSet("categories", new HashSet<>());
        categories.clear();
        categories.addAll(categoriesSet);
        setupCategorySpinner();
    }


    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString();
            if (!amountStr.isEmpty()) {
                if (categories.isEmpty()) {
                    showToast("No categories available. Cannot save expense.");
                } else {
                    double amount = Double.parseDouble(amountStr);
                    String category = (String) categorySpinner.getSelectedItem();
                    long timestamp = System.currentTimeMillis();
                    sendExpenseToPhone(amount, category, timestamp);
                    resetForm();
                }
            } else {
                showToast("Please enter a valid amount");
            }
        });
    }

    private void sendExpenseToPhone(double amount, String category, long timestamp) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/expense");
        putDataMapReq.getDataMap().putDouble("amount", amount);
        putDataMapReq.getDataMap().putString("category", category);
        putDataMapReq.getDataMap().putLong("timestamp", timestamp);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);
        putDataTask.addOnSuccessListener(dataItem -> {
            Log.d(TAG, "Expense sent successfully: Amount=" + amount + ", Category=" + category + ", Timestamp=" + timestamp);
            showToast("Expense saved");
        });
        putDataTask.addOnFailureListener(e -> {
            Log.e(TAG, "Failed to send expense", e);
            showToast("Failed to save expense");
        });
    }

    private void resetForm() {
        amountInput.getText().clear();
        categorySpinner.setSelection(0);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                        runOnUiThread(() -> updateCategories(newCategories));
                    }
                }
            }
        }
    }

    private void updateCategories(String[] newCategories) {
        categories.clear();
        categories.addAll(Arrays.asList(newCategories));
        setupCategorySpinner();
        Log.d(TAG, "Categories updated: " + categories);
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

        Log.d(TAG, "Category spinner updated with categories: " + categories);
    }

    private void requestCategoriesFromPhone() {
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    for (Node node : nodes) {
                        Wearable.getMessageClient(this).sendMessage(node.getId(), "/request_categories", new byte[0])
                                .addOnSuccessListener(messageId -> Log.d(TAG, "Requested categories from phone"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to request categories", e));
                    }
                });
    }

    private void checkPhoneConnection() {
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    if (!nodes.isEmpty()) {
                        for (Node node : nodes) {
                            Log.d(TAG, "Connected to phone: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                        }
                        requestCategoriesFromPhone();
                    } else {
                        Log.w(TAG, "Not connected to phone");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get connected nodes", e));
    }

}