package com.example.pocketledger;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryLimitAdapter extends RecyclerView.Adapter<CategoryLimitAdapter.ViewHolder> {
    private static final String TAG = "CategoryLimitAdapter";
    private List<CategoryLimit> categoryLimits;
    private OnLimitSaveListener onLimitSaveListener;

    public interface OnLimitSaveListener {
        void onLimitSave(CategoryLimit categoryLimit);
    }

    public CategoryLimitAdapter(List<CategoryLimit> categoryLimits, OnLimitSaveListener listener) {
        this.categoryLimits = new ArrayList<>(categoryLimits);
        this.onLimitSaveListener = listener;
        Log.d(TAG, "Adapter initialized with limits: " + this.categoryLimits);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_limit, parent, false);
        Log.d(TAG, "ViewHolder created");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryLimit limit = categoryLimits.get(position);
        holder.categoryTextView.setText(limit.getCategory());
        holder.limitEditText.setText(String.format(Locale.getDefault(), "%.2f", limit.getLimitAmount()));

        Log.d(TAG, "Binding view for category: " + limit.getCategory() + " with limit: " + limit.getLimitAmount());

        holder.limitEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double newLimit = Double.parseDouble(s.toString());
                    limit.setLimitAmount(newLimit);
                    Log.d(TAG, "Updated limit for " + limit.getCategory() + " to " + newLimit);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid input for category: " + limit.getCategory());
                }
            }
        });

        holder.saveButton.setOnClickListener(v -> {
            onLimitSaveListener.onLimitSave(limit);
        });
    }

    @Override
    public int getItemCount() {
        return categoryLimits.size();
    }

    public void updateLimits(List<CategoryLimit> newLimits) {
        this.categoryLimits.clear();
        this.categoryLimits.addAll(newLimits);
        notifyDataSetChanged();
        Log.d(TAG, "Adapter updated, new item count: " + getItemCount());
    }

    public List<CategoryLimit> getUpdatedLimits() {
        return new ArrayList<>(categoryLimits);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView;
        EditText limitEditText;
        Button saveButton;

        ViewHolder(View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            limitEditText = itemView.findViewById(R.id.limitEditText);
            saveButton = itemView.findViewById(R.id.saveButton);
        }
    }
}