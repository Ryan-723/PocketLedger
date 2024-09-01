package com.example.pocketledger;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CategoryLimitActivity extends AppCompatActivity implements CategoryLimitAdapter.OnLimitSaveListener {

    private static final String TAG = "CategoryLimitActivity";
    private RecyclerView categoryLimitRecyclerView;
    private CategoryLimitAdapter adapter;
    private List<CategoryLimit> categoryLimits;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_limit);

        Log.d(TAG, "onCreate called");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Category Spending Limits");
        }

        db = AppDatabase.getInstance(this);
        categoryLimitRecyclerView = findViewById(R.id.categoryLimitRecyclerView);
        Button saveAllLimitsButton = findViewById(R.id.saveLimitsButton);

        categoryLimits = new ArrayList<>();
        adapter = new CategoryLimitAdapter(categoryLimits, this);
        categoryLimitRecyclerView.setAdapter(adapter);
        categoryLimitRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadCategoryLimits();

        saveAllLimitsButton.setOnClickListener(v -> saveAllLimits());
    }

    private void loadCategoryLimits() {
        Log.d(TAG, "loadCategoryLimits called");
        new Thread(() -> {
            List<String> categories = db.categoryDao().getAllCategories();
            List<CategoryLimit> limits = db.categoryLimitDao().getAllCategoryLimits();

            Log.d(TAG, "Categories from database: " + categories);
            Log.d(TAG, "Existing limits from database: " + limits);

            List<CategoryLimit> updatedLimits = new ArrayList<>();
            for (String category : categories) {
                CategoryLimit limit = limits.stream()
                        .filter(l -> l.getCategory().equals(category))
                        .findFirst()
                        .orElse(new CategoryLimit(category, 0.0));
                updatedLimits.add(limit);
                Log.d(TAG, "Added category limit: " + limit);
            }

            Log.d(TAG, "Final category limits: " + updatedLimits);

            runOnUiThread(() -> {
                adapter.updateLimits(updatedLimits);
                Log.d(TAG, "Adapter updated with new limits. Item count: " + adapter.getItemCount());
            });
        }).start();
    }

    private void saveAllLimits() {
        Log.d(TAG, "saveAllLimits called");
        new Thread(() -> {
            List<CategoryLimit> updatedLimits = adapter.getUpdatedLimits();
            for (CategoryLimit limit : updatedLimits) {
                db.categoryLimitDao().insertOrUpdate(limit);
                Log.d(TAG, "Saved limit: " + limit);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "All limits saved", Toast.LENGTH_SHORT).show();
                loadCategoryLimits(); // Reload to reflect changes
            });
        }).start();
    }

    @Override
    public void onLimitSave(CategoryLimit categoryLimit) {
        Log.d(TAG, "onLimitSave called for: " + categoryLimit);
        new Thread(() -> {
            db.categoryLimitDao().insertOrUpdate(categoryLimit);
            Log.d(TAG, "Saved individual limit: " + categoryLimit);
            runOnUiThread(() -> {
                Toast.makeText(this, "Limit saved for " + categoryLimit.getCategory(), Toast.LENGTH_SHORT).show();
                loadCategoryLimits(); // Reload to reflect changes
            });
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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        loadCategoryLimits(); // Reload limits when returning to this activity
    }
}