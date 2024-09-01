package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    List<Expense> getAllExpenses();

    @Insert
    long insertExpense(Expense expense);

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category")
    double getCategoryTotal(String category);

    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category ORDER BY total DESC")
    List<CategoryTotal> getExpenseTotalsByCategory();

    @Query("DELETE FROM expenses")
    int deleteAllExpenses();

    @Delete
    void deleteExpense(Expense expense);

    @Query("SELECT * FROM expenses WHERE " +
            "(:month = 'All' OR strftime('%m', datetime(timestamp / 1000, 'unixepoch')) = :monthNumber) AND " +
            "(:year = 'All' OR strftime('%Y', datetime(timestamp / 1000, 'unixepoch')) = :year) " +
            "ORDER BY timestamp DESC")
    List<Expense> getFilteredExpenses(String month, String monthNumber, String year);

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE " +
            "(:month = 'All' OR strftime('%m', datetime(timestamp / 1000, 'unixepoch')) = :monthNumber) AND " +
            "(:year = 'All' OR strftime('%Y', datetime(timestamp / 1000, 'unixepoch')) = :year) " +
            "GROUP BY category ORDER BY total DESC")
    List<CategoryTotal> getFilteredExpenseTotalsByCategory(String month, String monthNumber, String year);
}