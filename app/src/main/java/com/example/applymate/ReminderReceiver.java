package com.example.applymate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ReminderScheduler.ACTION_REMINDER.equals(intent.getAction())) {
            return;
        }
        long reminderId = intent.getLongExtra("reminder_id", -1L);
        if (reminderId < 1) {
            return;
        }
        ReminderNotifications.show(
                context,
                reminderId,
                intent.getStringExtra("reminder_title"),
                intent.getStringExtra("reminder_description"));
    }
}
