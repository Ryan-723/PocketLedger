package com.example.pocketledger;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
}