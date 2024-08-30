package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    List<Expense> getAllExpenses();

    @Insert
    void insertExpense(Expense expense);

    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category ORDER BY total DESC")
    List<CategoryTotal> getExpenseTotalsByCategory();

    @Query("DELETE FROM expenses")
    int deleteAllExpenses();
}