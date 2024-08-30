package com.example.pocketledger;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    public static final String ACTION_DATA_CLEARED = "com.example.pocketledger.ACTION_DATA_CLEARED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button clearAllDataButton = findViewById(R.id.clearAllDataButton);
        Button clearExpensesButton = findViewById(R.id.clearExpensesButton);
        Button clearCategoriesButton = findViewById(R.id.clearCategoriesButton);

        clearAllDataButton.setOnClickListener(v -> showClearDataConfirmationDialog("all data", this::clearAllData));
        clearExpensesButton.setOnClickListener(v -> showClearDataConfirmationDialog("all expenses", this::clearExpenses));
        clearCategoriesButton.setOnClickListener(v -> showClearDataConfirmationDialog("all categories", this::clearCategories));
    }

    private void showClearDataConfirmationDialog(String dataType, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle("Clear " + dataType)
                .setMessage("Are you sure you want to clear " + dataType + "? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void clearAllData() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                int expensesDeleted = db.expenseDao().deleteAllExpenses();
                int categoriesDeleted = db.categoryDao().deleteAllCategories();

                Log.d(TAG, "Expenses deleted: " + expensesDeleted);
                Log.d(TAG, "Categories deleted: " + categoriesDeleted);

                runOnUiThread(() -> {
                    Toast.makeText(this, "All data cleared. Expenses: " + expensesDeleted + ", Categories: " + categoriesDeleted, Toast.LENGTH_LONG).show();
                    notifyDataCleared();
                });
            } catch (Exception e) {
                handleClearDataError(e);
            }
        }).start();
    }

    private void clearExpenses() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                int expensesDeleted = db.expenseDao().deleteAllExpenses();

                Log.d(TAG, "Expenses deleted: " + expensesDeleted);

                runOnUiThread(() -> {
                    Toast.makeText(this, "All expenses cleared: " + expensesDeleted, Toast.LENGTH_LONG).show();
                    notifyDataCleared();
                });
            } catch (Exception e) {
                handleClearDataError(e);
            }
        }).start();
    }

    private void clearCategories() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                int categoriesDeleted = db.categoryDao().deleteAllCategories();

                Log.d(TAG, "Categories deleted: " + categoriesDeleted);

                runOnUiThread(() -> {
                    Toast.makeText(this, "All categories cleared: " + categoriesDeleted, Toast.LENGTH_LONG).show();
                    notifyDataCleared();
                    sendEmptyCategoryListToWear();
                });
            } catch (Exception e) {
                handleClearDataError(e);
            }
        }).start();
    }

    private void sendEmptyCategoryListToWear() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/categories");
        putDataMapReq.getDataMap().putStringArray("categories", new String[0]);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();

        Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);

        putDataTask.addOnSuccessListener(dataItem -> {
            Log.d(TAG, "Empty category list sent successfully to Wear");
        });

        putDataTask.addOnFailureListener(exception -> {
            Log.e(TAG, "Sending empty category list to Wear failed: " + exception);
        });
    }

    private void handleClearDataError(Exception e) {
        Log.e(TAG, "Error clearing data", e);
        runOnUiThread(() -> {
            Toast.makeText(this, "Error clearing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void notifyDataCleared() {
        Intent intent = new Intent(ACTION_DATA_CLEARED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}