package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT name FROM categories")
    List<String> getAllCategories();

    @Insert
    void insert(Category category);

    @Query("DELETE FROM categories")
    int deleteAllCategories();
}