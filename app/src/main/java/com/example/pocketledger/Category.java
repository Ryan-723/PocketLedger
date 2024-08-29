package com.example.pocketledger;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Entity class representing a category in the database
@Entity(tableName = "categories")
public class Category {
    @PrimaryKey
    @NonNull
    private String name;

    public Category(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }
}