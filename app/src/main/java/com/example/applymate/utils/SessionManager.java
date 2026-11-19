package com.example.applymate.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFERENCES_NAME = "applymate_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_LOGGED_IN = "logged_in";
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void createSession(long userId) {
        preferences.edit()
                .putLong(KEY_USER_ID, userId)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false)
                && getCurrentUserId() > 0;
    }

    public long getCurrentUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
