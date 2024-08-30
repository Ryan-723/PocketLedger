package com.example.pocketledger;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerService";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/request_categories")) {
            sendCategoriesToWear();
        }
    }

    private void sendCategoriesToWear() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<String> categories = db.categoryDao().getAllCategories();

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/categories");
            putDataMapReq.getDataMap().putStringArray("categories", categories.toArray(new String[0]));
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();

            Task<DataItem> putDataTask = Wearable.getDataClient(this).putDataItem(putDataReq);

            putDataTask.addOnSuccessListener(dataItem -> {
                Log.d(TAG, "Categories sent successfully to Wear: " + categories);
            });

            putDataTask.addOnFailureListener(exception -> {
                Log.e(TAG, "Sending categories to Wear failed: " + exception);
            });
        }).start();
    }
}
