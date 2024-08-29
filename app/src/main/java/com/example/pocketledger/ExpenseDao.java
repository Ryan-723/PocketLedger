package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

// Data Access Object (DAO) for Expense-related database operations
@Dao
public interface ExpenseDao {
    // Retrieve all expenses, ordered by most recent first
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    List<Expense> getAllExpenses();

    // Insert a new expense into the database
    @Insert
    void insertExpense(Expense expense);

    // Get total expenses grouped by category, ordered by highest total first
    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category ORDER BY total DESC")
    List<CategoryTotal> getExpenseTotalsByCategory();
}