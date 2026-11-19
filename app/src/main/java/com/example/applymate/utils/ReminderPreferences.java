package com.example.applymate.utils;

import android.content.Context;
import android.content.SharedPreferences;

public final class ReminderPreferences {

    private static final String PREFERENCES_NAME = "applymate_preferences";
    private static final String KEY_REMINDER_ALERTS_ENABLED = "reminder_alerts_enabled";

    private ReminderPreferences() {
    }

    public static boolean areReminderAlertsEnabled(Context context) {
        return preferences(context).getBoolean(KEY_REMINDER_ALERTS_ENABLED, true);
    }

    public static void setReminderAlertsEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_REMINDER_ALERTS_ENABLED, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
