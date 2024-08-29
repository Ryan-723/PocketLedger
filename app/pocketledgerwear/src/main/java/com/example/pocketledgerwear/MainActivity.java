package com.example.pocketledgerwear;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {

    private static final String TAG = "WearMainActivity";
    private EditText amountInput;
    private Spinner categorySpinner;
    private Button saveButton;
    private List<String> categories = new ArrayList<>(Arrays.asList("Food", "Transportation", "Entertainment", "Utilities"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        amountInput = findViewById(R.id.amountInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);

        // Set up the UI components
        setupCategorySpinner();
        setupSaveButton();
        checkPhoneConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register listeners for data and message events
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister listeners when the activity is paused
        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
    }

    private void setupCategorySpinner() {
        // Set up the category spinner with available categories
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    // Set up the save button to send expense data to the phone
    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                String category = (String) categorySpinner.getSelectedItem();
                long timestamp = System.currentTimeMillis();
                sendExpenseToPhone(amount, category, timestamp);
                resetForm();
            } else {
                showToast("Please enter a valid amount");
            }
        });
    }

    // Send expense data to the connected phone
    private void sendExpenseToPhone(double amount, String category, long timestamp) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/expense");
        putDataMapReq.getDataMap().putDouble("amount", amount);
        putDataMapReq.getDataMap().putString("category", category);
        putDataMapReq.getDataMap().putLong("timestamp", timestamp);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);
        putDataTask.addOnSuccessListener(dataItem -> {
            Log.d(TAG, "Expense sent successfully");
            showToast("Expense saved");
        });
        putDataTask.addOnFailureListener(e -> {
            Log.e(TAG, "Failed to send expense", e);
            showToast("Failed to save expense");
        });
    }

    // Reset the input form after sending an expense
    private void resetForm() {
        amountInput.getText().clear();
        categorySpinner.setSelection(0);
    }

    // Display a toast message
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Check the connection to the phone
    private void checkPhoneConnection() {
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    if (!nodes.isEmpty()) {
                        for (Node node : nodes) {
                            Log.d(TAG, "Connected to phone: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                        }
                        sendTestMessage();
                    } else {
                        Log.w(TAG, "Not connected to phone");
                        showToast("Not connected to phone");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get connected nodes", e));
    }

    // Send a test message to the connected phone
    private void sendTestMessage() {
        Wearable.getNodeClient(this).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    for (Node node : nodes) {
                        Wearable.getMessageClient(this).sendMessage(node.getId(), "/test", "Hello from Wear".getBytes())
                                .addOnSuccessListener(messageId -> Log.d(TAG, "Test message sent successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to send test message", e));
                    }
                });
    }

    // Handle data changes received from the phone
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals("/categories")) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String[] newCategories = dataMap.getStringArray("categories");
                    if (newCategories != null) {
                        updateCategories(newCategories);
                    }
                }
            }
        }
    }

    // Update the categories list and refresh the spinner
    private void updateCategories(String[] newCategories) {
        categories.clear();
        categories.addAll(Arrays.asList(newCategories));
        runOnUiThread(this::setupCategorySpinner);
    }

    // Handle messages received from the phone
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/message")) {
            String message = new String(messageEvent.getData());
            Log.d(TAG, "Received message: " + message);
            runOnUiThread(() -> showToast(message));
        }
    }
}