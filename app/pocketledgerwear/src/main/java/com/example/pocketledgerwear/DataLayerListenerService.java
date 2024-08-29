package com.example.pocketledgerwear;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Arrays;
import java.util.HashSet;

// Service to listen for data changes from the phone in the background
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerListenerService";

    // Handle data changes received from the phone
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged called");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                Log.d(TAG, "Data item changed: " + item.getUri());
                if (item.getUri().getPath().equals("/categories")) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                    String[] newCategories = dataMapItem.getDataMap().getStringArray("categories");
                    Log.d(TAG, "Received new categories: " + Arrays.toString(newCategories));
                    updateCategories(newCategories);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "Data item deleted: " + event.getDataItem().getUri());
                // DataItem deleted
            }
        }
    }

    // Update categories in SharedPreferences and notify the app
    private void updateCategories(String[] newCategories) {
        Log.d(TAG, "Updating categories in SharedPreferences");
        SharedPreferences prefs = getSharedPreferences("CategoryPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("categories", new HashSet<>(Arrays.asList(newCategories)));
        boolean success = editor.commit();
        Log.d(TAG, "Categories update " + (success ? "successful" : "failed"));

        // Send a broadcast to notify the app about the category update
        Intent updateIntent = new Intent("com.example.budgetpulsewear.UPDATE_CATEGORIES");
        sendBroadcast(updateIntent);
        Log.d(TAG, "Broadcast sent to update categories");
    }
}