package com.example.pocketledger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ExpenseLimitReceiver extends BroadcastReceiver {
    private static final String TAG = "ExpenseLimitReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String category = intent.getStringExtra("category");
        double total = intent.getDoubleExtra("total", 0);
        double limit = intent.getDoubleExtra("limit", 0);

        int percentage = (int)((total/limit) * 100);
        String message = percentage >= 100
                ? "You've exceeded your limit for " + category
                : "You've reached " + percentage + "% of your limit for " + category;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "expense_limit_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Expense Limit Alert")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(category.hashCode(), builder.build());
            } else {
                Log.e(TAG, "Notification permission not granted");
            }
        } else {
            notificationManager.notify(category.hashCode(), builder.build());
        }
    }
}