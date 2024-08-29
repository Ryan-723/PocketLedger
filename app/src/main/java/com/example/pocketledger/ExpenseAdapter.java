package com.example.pocketledger;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adapter for displaying expenses in a RecyclerView
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
    private static final String TAG = "ExpenseAdapter";
    private List<Expense> expenses;

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
        Log.d(TAG, "ExpenseAdapter created with " + expenses.size() + " expenses");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        Log.d(TAG, "ViewHolder created");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.amountTextView.setText(String.format("$%.2f", expense.getAmount()));
        holder.categoryTextView.setText(expense.getCategory());
        holder.dateTextView.setText(new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(new Date(expense.getTimestamp())));
        Log.d(TAG, "Bound ViewHolder at position " + position + ": " + expense.getAmount() + " - " + expense.getCategory());
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    // ViewHolder class to hold references to the views for each expense item
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView;
        TextView categoryTextView;
        TextView dateTextView;

        ViewHolder(View view) {
            super(view);
            amountTextView = view.findViewById(R.id.amountTextView);
            categoryTextView = view.findViewById(R.id.categoryTextView);
            dateTextView = view.findViewById(R.id.dateTextView);
        }
    }
}