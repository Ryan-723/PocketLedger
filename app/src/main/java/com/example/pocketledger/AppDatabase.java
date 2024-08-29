package com.example.pocketledger;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// This class sets up the Room database for the app
@Database(entities = {Expense.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";

    public abstract ExpenseDao expenseDao();

    private static AppDatabase instance;

    // Migration objects handle database schema changes between versions
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
        }
    };

    // to ensure only one database instance is created
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "Creating new AppDatabase instance");
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "expense_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build();
            Log.d(TAG, "AppDatabase instance created successfully");
        }
        return instance;
    }
}