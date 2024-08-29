package com.example.pocketledger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter for displaying category totals in a RecyclerView
public class CategoryTotalAdapter extends RecyclerView.Adapter<CategoryTotalAdapter.ViewHolder> {
    private List<CategoryTotal> categoryTotals;

    public CategoryTotalAdapter(List<CategoryTotal> categoryTotals) {
        this.categoryTotals = categoryTotals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_total_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryTotal categoryTotal = categoryTotals.get(position);
        holder.categoryNameTextView.setText(categoryTotal.category);
        holder.categoryTotalTextView.setText(String.format("$%.2f", categoryTotal.total));
    }

    @Override
    public int getItemCount() {
        return categoryTotals.size();
    }

    // ViewHolder class to hold references to the views for each item
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;
        TextView categoryTotalTextView;

        ViewHolder(View view) {
            super(view);
            categoryNameTextView = view.findViewById(R.id.categoryNameTextView);
            categoryTotalTextView = view.findViewById(R.id.categoryTotalTextView);
        }
    }
}