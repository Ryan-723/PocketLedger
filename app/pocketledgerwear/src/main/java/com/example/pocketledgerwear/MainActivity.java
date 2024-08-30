package com.example.pocketledgerwear;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        amountInput = findViewById(R.id.amountInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveButton = findViewById(R.id.saveButton);

        setupCategorySpinner();
        setupSaveButton();
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

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

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

    private void sendExpenseToPhone(double amount, String category, long timestamp) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/expense");
        putDataMapReq.getDataMap().putDouble("amount", amount);
        putDataMapReq.getDataMap().putString("category", category);
        putDataMapReq.getDataMap().putLong("timestamp", timestamp);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Wearable.getDataClient(this).putDataItem(putDataReq)
                .addOnSuccessListener(dataItem -> {
                    Log.d(TAG, "Expense sent successfully");
                    showToast("Expense saved");
                })
                .addOnFailureListener(e -> {
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
                        updateCategories(newCategories);
                    }
                }
            }
        }
    }

    private void updateCategories(String[] newCategories) {
        categories.clear();
        categories.addAll(Arrays.asList(newCategories));
        runOnUiThread(this::setupCategorySpinner);
    }
}