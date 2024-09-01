package com.example.pocketledger;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CategoryLimit categoryLimit);

    @Query("SELECT * FROM category_limits WHERE category = :category")
    CategoryLimit getCategoryLimit(String category);

    @Query("SELECT * FROM category_limits")
    List<CategoryLimit> getAllCategoryLimits();

    @Query("DELETE FROM category_limits")
    int deleteAllCategoryLimits();
}