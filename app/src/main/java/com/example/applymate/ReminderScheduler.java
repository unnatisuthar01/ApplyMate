package com.example.applymate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.applymate.models.Reminder;

import java.util.Calendar;

final class ReminderScheduler {

    static final String ACTION_REMINDER =
            "com.example.applymate.action.SHOW_REMINDER";
    private static final String EXTRA_TITLE = "reminder_title";
    private static final String EXTRA_DESCRIPTION = "reminder_description";

    private ReminderScheduler() {
    }

    static void schedule(Context context, Reminder reminder) {
        long triggerAt = getTriggerAt(reminder.getDate(), reminder.getTime());
        if (triggerAt <= System.currentTimeMillis()
                || "Completed".equals(reminder.getStatus())) {
            cancel(context, reminder.getId());
            return;
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            throw new IllegalStateException("Alarm service is unavailable.");
        }
        PendingIntent pendingIntent = createPendingIntent(context, reminder);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    static void cancel(Context context, long reminderId) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createPendingIntent(context, reminderId, null, null));
        }
    }

    static long getTriggerAt(long reminderDate, String time) {
        Calendar trigger = Calendar.getInstance();
        trigger.setTimeInMillis(reminderDate);
        String[] parts = time == null ? new String[0] : time.split(":");
        if (parts.length != 2) {
            if (time == null || time.isEmpty()) {
                trigger.set(Calendar.HOUR_OF_DAY, 9);
                trigger.set(Calendar.MINUTE, 0);
                trigger.set(Calendar.SECOND, 0);
                trigger.set(Calendar.MILLISECOND, 0);
                return trigger.getTimeInMillis();
            }
            throw new IllegalArgumentException("Reminder time must use HH:mm format.");
        }
        try {
            trigger.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            trigger.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Reminder time must use HH:mm format.", exception);
        }
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        return trigger.getTimeInMillis();
    }

    private static PendingIntent createPendingIntent(Context context, Reminder reminder) {
        return createPendingIntent(
                context, reminder.getId(), reminder.getTitle(), reminder.getDescription());
    }

    private static PendingIntent createPendingIntent(
            Context context, long reminderId, String title, String description) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);
        intent.putExtra("reminder_id", reminderId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(
                context, (int) (reminderId ^ (reminderId >>> 32)), intent, flags);
    }
}
