package com.example.applymate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.applymate.utils.ReminderPreferences;

final class ReminderNotifications {

    private static final String CHANNEL_ID = "applymate_reminders";

    private ReminderNotifications() {
    }

    static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean areNotificationsEnabled(Context context) {
        return hasPermission(context)
                && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    static void show(Context context, long reminderId, String title, String description) {
        if (!areNotificationsEnabled(context)
                || !ReminderPreferences.areReminderAlertsEnabled(context)) {
            return;
        }
        createChannel(context);
        Intent intent = new Intent(context, ReminderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, (int) (reminderId ^ (reminderId >>> 32)), intent, flags);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? context.getString(R.string.reminder_notification_title) : title)
                .setContentText(description == null || description.trim().isEmpty()
                        ? context.getString(R.string.reminder_notification_default) : description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        description == null || description.trim().isEmpty()
                                ? context.getString(R.string.reminder_notification_default)
                                : description))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(
                    (int) reminderId, notification.build());
        } catch (SecurityException exception) {
            android.util.Log.w("ApplyMate", "Notification permission is not available.", exception);
        }
    }

    static void cancel(Context context, long reminderId) {
        NotificationManagerCompat.from(context).cancel((int) reminderId);
    }
}
