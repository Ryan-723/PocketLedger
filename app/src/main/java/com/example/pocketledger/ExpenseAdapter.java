package com.example.pocketledger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
    private List<Expense> expenses;
    private OnExpenseDeleteListener deleteListener;

    public interface OnExpenseDeleteListener {
        void onExpenseDelete(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenses, OnExpenseDeleteListener deleteListener) {
        this.expenses = expenses;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.amountTextView.setText(String.format("$%.2f", expense.getAmount()));
        holder.categoryTextView.setText(expense.getCategory());
        holder.dateTextView.setText(new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()).format(new Date(expense.getTimestamp())));

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onExpenseDelete(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView;
        TextView categoryTextView;
        TextView dateTextView;
        Button deleteButton;

        ViewHolder(View view) {
            super(view);
            amountTextView = view.findViewById(R.id.amountTextView);
            categoryTextView = view.findViewById(R.id.categoryTextView);
            dateTextView = view.findViewById(R.id.dateTextView);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}