package com.example.applymate;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;
import com.example.applymate.utils.ReminderPreferences;
import com.example.applymate.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private long userId;
    private SwitchMaterial reminderAlertsSwitch;
    private TextView notificationStatus;
    private boolean updatingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        reminderAlertsSwitch = findViewById(R.id.switch_reminder_alerts);
        notificationStatus = findViewById(R.id.text_notification_settings_status);
        reminderAlertsSwitch.setChecked(ReminderPreferences.areReminderAlertsEnabled(this));
        reminderAlertsSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingSwitch) {
                updateReminderPreference(checked);
            }
        });
        findViewById(R.id.button_manage_notifications)
                .setOnClickListener(view -> openNotificationSettings());
        displayAppVersion();
        refreshNotificationStatus();
        ScreenNavigation.configureBottomNavigation(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationStatus != null) {
            refreshNotificationStatus();
        }
    }

    private void updateReminderPreference(boolean enabled) {
        boolean previousValue = ReminderPreferences.areReminderAlertsEnabled(this);
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            List<Reminder> reminders = database.getActiveReminders(userId);
            ReminderPreferences.setReminderAlertsEnabled(this, enabled);
            for (Reminder reminder : reminders) {
                ReminderScheduler.schedule(this, reminder);
                if (!enabled) {
                    ReminderNotifications.cancel(this, reminder.getId());
                }
            }
        } catch (SQLiteException | IllegalStateException | SecurityException exception) {
            ReminderPreferences.setReminderAlertsEnabled(this, previousValue);
            restoreReminderAlarms(previousValue);
            updatingSwitch = true;
            reminderAlertsSwitch.setChecked(previousValue);
            updatingSwitch = false;
            Toast.makeText(this, R.string.reminder_preference_error, Toast.LENGTH_LONG).show();
        }
    }

    private void restoreReminderAlarms(boolean alertsEnabled) {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            for (Reminder reminder : database.getActiveReminders(userId)) {
                if (alertsEnabled) {
                    ReminderScheduler.schedule(this, reminder);
                } else {
                    ReminderScheduler.cancel(this, reminder.getId());
                }
            }
        } catch (SQLiteException | IllegalStateException | SecurityException exception) {
            android.util.Log.e("ApplyMate", "Could not restore reminder alarm settings.", exception);
        }
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        }
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.notification_settings_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshNotificationStatus() {
        notificationStatus.setText(ReminderNotifications.areNotificationsEnabled(this)
                ? R.string.notifications_system_enabled
                : R.string.notifications_system_disabled);
    }

    private void displayAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName == null
                    ? getString(R.string.unknown_value) : packageInfo.versionName;
            ((TextView) findViewById(R.id.text_app_version))
                    .setText(getString(R.string.app_version_value, versionName));
        } catch (PackageManager.NameNotFoundException exception) {
            ((TextView) findViewById(R.id.text_app_version))
                    .setText(getString(R.string.app_version_unknown));
        }
    }
}
