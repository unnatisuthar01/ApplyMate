package com.example.applymate;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ReminderActivity extends AppCompatActivity {

    public static final String EXTRA_REMINDER_ID =
            "com.example.applymate.extra.REMINDER_ID";
    private long userId;
    private ReminderAdapter adapter;
    private Spinner filterSpinner;
    private View emptyState;
    private TextView notificationPermissionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        filterSpinner = findViewById(R.id.spinner_reminder_filter);
        emptyState = findViewById(R.id.layout_reminders_empty);
        notificationPermissionText = findViewById(R.id.text_notification_permission);
        RecyclerView recyclerView = findViewById(R.id.recycler_reminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(new ArrayList<>(), new ReminderAdapter.Listener() {
            @Override
            public void onEdit(Reminder reminder) {
                openReminder(reminder.getId());
            }

            @Override
            public void onComplete(Reminder reminder) {
                completeReminder(reminder);
            }

            @Override
            public void onDelete(Reminder reminder) {
                confirmDelete(reminder);
            }
        });
        recyclerView.setAdapter(adapter);
        filterSpinner.setAdapter(android.widget.ArrayAdapter.createFromResource(
                this, R.array.reminder_filters, android.R.layout.simple_spinner_item));
        ((android.widget.ArrayAdapter<?>) filterSpinner.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadReminders();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        findViewById(R.id.button_add_reminder).setOnClickListener(view -> openReminder(-1L));
        findViewById(R.id.button_enable_notifications)
                .setOnClickListener(view -> requestNotificationPermission());
        findViewById(R.id.button_enable_exact_alarms)
                .setOnClickListener(view -> requestExactAlarmAccess());
        ScreenNavigation.configureBottomNavigation(this, false);
        refreshNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && userId > 0) {
            loadReminders();
            refreshNotificationPermission();
            rescheduleRemindersIfExactAccessGranted();
        }
    }

    private void loadReminders() {
        if (adapter == null) {
            return;
        }
        String selection = (String) filterSpinner.getSelectedItem();
        String filter = getString(R.string.reminder_filter_all).equals(selection)
                ? null : selection;
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            java.util.List<Reminder> reminders =
                    database.getRemindersForUser(userId, filter);
            adapter.submitList(reminders);
            emptyState.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void openReminder(long reminderId) {
        Intent intent = new Intent(this, AddEditReminderActivity.class);
        if (reminderId > 0) {
            intent.putExtra(EXTRA_REMINDER_ID, reminderId);
        }
        startActivity(intent);
    }

    private void completeReminder(Reminder reminder) {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            if (database.setReminderStatus(reminder.getId(), userId, "Completed")) {
                ReminderScheduler.cancel(this, reminder.getId());
                loadReminders();
            }
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.reminder_save_error, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reminder_delete_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.confirm_delete, (dialog, which) -> {
                    try (DatabaseHelper database = new DatabaseHelper(this)) {
                        if (database.deleteReminder(reminder.getId(), userId)) {
                            ReminderScheduler.cancel(this, reminder.getId());
                            loadReminders();
                        }
                    } catch (SQLiteException exception) {
                        Toast.makeText(this, R.string.reminder_save_error, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void refreshNotificationPermission() {
        boolean enabled = ReminderNotifications.areNotificationsEnabled(this);
        notificationPermissionText.setText(enabled
                ? R.string.reminder_notifications_enabled
                : R.string.reminder_notifications_disabled);
        findViewById(R.id.button_enable_notifications)
                .setVisibility(enabled ? View.GONE : View.VISIBLE);
        boolean exactAccessRequired = android.os.Build.VERSION.SDK_INT >= 31;
        boolean exactAccessGranted = !exactAccessRequired
                || ((android.app.AlarmManager) getSystemService(ALARM_SERVICE))
                .canScheduleExactAlarms();
        ((TextView) findViewById(R.id.text_exact_alarm_permission)).setText(exactAccessGranted
                ? R.string.reminder_exact_timing_enabled
                : R.string.reminder_exact_timing_disabled);
        findViewById(R.id.button_enable_exact_alarms)
                .setVisibility(exactAccessGranted ? View.GONE : View.VISIBLE);
    }

    private void requestExactAlarmAccess() {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return;
        }
        Intent settingsIntent = new Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        settingsIntent.setData(android.net.Uri.parse("package:" + getPackageName()));
        try {
            startActivity(settingsIntent);
        } catch (android.content.ActivityNotFoundException exception) {
            Intent appSettings = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            appSettings.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(appSettings);
        }
    }

    private void rescheduleRemindersIfExactAccessGranted() {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            return;
        }
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null || !alarmManager.canScheduleExactAlarms()) {
            return;
        }
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            for (Reminder reminder : database.getActiveReminders(userId)) {
                ReminderScheduler.schedule(this, reminder);
            }
        } catch (SQLiteException | IllegalStateException | SecurityException exception) {
            Toast.makeText(this, R.string.reminder_alarm_error, Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 210);
        } else {
            refreshNotificationPermission();
            if (!androidx.core.app.NotificationManagerCompat.from(this)
                    .areNotificationsEnabled()) {
                Intent settingsIntent;
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    settingsIntent = new Intent(
                            android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    settingsIntent.putExtra(
                            android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                } else {
                    settingsIntent = new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    settingsIntent.setData(android.net.Uri.parse("package:" + getPackageName()));
                }
                startActivity(settingsIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 210) {
            refreshNotificationPermission();
            if (!ReminderNotifications.hasPermission(this)) {
                Toast.makeText(this, R.string.reminder_permission_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
