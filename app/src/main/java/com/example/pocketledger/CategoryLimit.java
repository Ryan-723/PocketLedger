package com.example.pocketledger;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_limits")
public class CategoryLimit {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "limit_amount")
    private double limitAmount;

    public CategoryLimit(@NonNull String category, double limitAmount) {
        this.category = category;
        this.limitAmount = limitAmount;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(double limitAmount) {
        this.limitAmount = limitAmount;
    }

    @Override
    public String toString() {
        return "CategoryLimit{" +
                "category='" + category + '\'' +
                ", limitAmount=" + limitAmount +
                '}';
    }
}