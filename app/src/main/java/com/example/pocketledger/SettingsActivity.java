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

        Button clearDataButton = findViewById(R.id.clearDataButton);
        clearDataButton.setOnClickListener(v -> showClearDataConfirmationDialog());
    }

    private void showClearDataConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to clear all data? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearAllData())
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

                    // Send broadcast to notify MainActivity
                    Intent intent = new Intent(ACTION_DATA_CLEARED);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                    // Finish the activity
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error clearing data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error clearing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
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