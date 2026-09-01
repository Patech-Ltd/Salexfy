package com.patechltd.salexfypos.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.ui.stock.LowStockActivity;

public final class Notifier {

    public static final String CHANNEL_STOCK = "stock_alerts";
    private static final int NOTIFICATION_LOW_STOCK = 1001;

    private Notifier() {
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_STOCK, "Stock alerts", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Low stock and inventory alerts");
        nm.createNotificationChannel(channel);
    }

    public static boolean notificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static void lowStock(Context context, int count) {
        if (count <= 0) return;
        final Context app = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannels(app);
        Intent intent = new Intent(app, LowStockActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(app, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(app, CHANNEL_STOCK)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Low stock")
                .setContentText(count + " item" + (count == 1 ? " is" : "s are")
                        + " running low on stock")
                .setContentIntent(pi)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(count + " item" + (count == 1 ? " is" : "s are")
                                + " running low on stock. Tap to view."))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        NotificationManager nm = app.getSystemService(NotificationManager.class);
        nm.notify(NOTIFICATION_LOW_STOCK, notification);
    }
}