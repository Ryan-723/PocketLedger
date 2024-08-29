package com.example.pocketledger;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Entity class representing an expense in the database
@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private String category;
    private long timestamp;

    public Expense(double amount, String category, long timestamp) {
        this.amount = amount;
        this.category = category;
        this.timestamp = timestamp;
    }

    // Getter and setter methods for expense properties
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public long getTimestamp() {
        return timestamp;
    }
}