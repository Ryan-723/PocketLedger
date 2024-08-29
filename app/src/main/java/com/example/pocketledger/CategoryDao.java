package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

// Data Access Object (DAO) for Category-related database operations
@Dao
public interface CategoryDao {
    @Query("SELECT name FROM categories")
    List<String> getAllCategories();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCategory(Category category);
}