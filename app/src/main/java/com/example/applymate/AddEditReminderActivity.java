package com.example.applymate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;
import com.example.applymate.utils.ReminderPreferences;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditReminderActivity extends AppCompatActivity {

    private long userId;
    private long reminderId;
    private long reminderDate;
    private String reminderTime;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText dateInput;
    private EditText timeInput;
    private Spinner typeSpinner;
    private TextView heading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        heading = findViewById(R.id.text_reminder_form_title);
        titleInput = findViewById(R.id.input_reminder_title);
        descriptionInput = findViewById(R.id.input_reminder_description);
        dateInput = findViewById(R.id.input_reminder_date);
        timeInput = findViewById(R.id.input_reminder_time);
        typeSpinner = findViewById(R.id.spinner_reminder_type);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.reminder_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 5);
        setDate(now);
        setTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        dateInput.setOnClickListener(view -> showDatePicker());
        timeInput.setOnClickListener(view -> showTimePicker());

        reminderId = getIntent().getLongExtra(ReminderActivity.EXTRA_REMINDER_ID, -1L);
        MaterialButton saveButton = findViewById(R.id.button_save_reminder);
        saveButton.setOnClickListener(view -> saveReminder());
        if (reminderId > 0) {
            heading.setText(R.string.edit_reminder_title);
            saveButton.setText(R.string.update_reminder);
            loadReminder();
        } else {
            heading.setText(R.string.add_reminder_title);
        }
    }

    private void loadReminder() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            Reminder reminder = database.getReminder(reminderId, userId);
            if (reminder == null) {
                Toast.makeText(this, R.string.reminder_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            titleInput.setText(reminder.getTitle());
            descriptionInput.setText(reminder.getDescription());
            setDate(reminder.getDate());
            String[] time = reminder.getTime() == null
                    ? new String[]{"09", "00"} : reminder.getTime().split(":");
            setTime(Integer.parseInt(time[0]), Integer.parseInt(time[1]));
            for (int position = 0; position < typeSpinner.getCount(); position++) {
                if (typeSpinner.getItemAtPosition(position).toString().equals(reminder.getType())) {
                    typeSpinner.setSelection(position);
                    break;
                }
            }
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showDatePicker() {
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(reminderDate);
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> {
                    Calendar date = Calendar.getInstance();
                    date.clear();
                    date.set(year, month, day);
                    setDate(date);
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar selected = Calendar.getInstance();
        String[] parts = reminderTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        new TimePickerDialog(this,
                (picker, selectedHour, selectedMinute) -> setTime(selectedHour, selectedMinute),
                hour, minute, false).show();
    }

    private void setDate(Calendar date) {
        Calendar normalized = (Calendar) date.clone();
        normalized.set(Calendar.HOUR_OF_DAY, 0);
        normalized.set(Calendar.MINUTE, 0);
        normalized.set(Calendar.SECOND, 0);
        normalized.set(Calendar.MILLISECOND, 0);
        reminderDate = normalized.getTimeInMillis();
        dateInput.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(new Date(reminderDate)));
    }

    private void setDate(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        setDate(calendar);
    }

    private void setTime(int hour, int minute) {
        reminderTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
        timeInput.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(
                new Date(ReminderScheduler.getTriggerAt(reminderDate, reminderTime))));
    }

    private void saveReminder() {
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            titleInput.setError(getString(R.string.required_field));
            return;
        }
        if (typeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, R.string.reminder_select_type, Toast.LENGTH_SHORT).show();
            return;
        }

        Reminder previous = null;
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            if (reminderId > 0) {
                previous = database.getReminder(reminderId, userId);
                if (previous == null) {
                    Toast.makeText(this, R.string.reminder_not_found, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            String status = previous == null ? "Upcoming" : previous.getStatus();
            Reminder reminder = new Reminder(
                    reminderId > 0 ? reminderId : 0L,
                    userId,
                    previous == null ? null : previous.getJobId(),
                    title,
                    descriptionInput.getText().toString().trim(),
                    typeSpinner.getSelectedItem().toString(),
                    reminderDate,
                    reminderTime,
                    status);
            if (reminderId > 0) {
                if (!database.updateReminder(reminder)) {
                    Toast.makeText(this, R.string.reminder_not_found, Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                reminderId = database.insertReminder(reminder);
                reminder = new Reminder(
                        reminderId, userId, null, title,
                        descriptionInput.getText().toString().trim(),
                        typeSpinner.getSelectedItem().toString(), reminderDate,
                        reminderTime, status);
            }
            if (previous != null) {
                ReminderScheduler.cancel(this, reminderId);
            }
            ReminderScheduler.schedule(this, reminder);
            setResult(RESULT_OK);
            int message = ReminderScheduler.getTriggerAt(
                    reminderDate, reminderTime) <= System.currentTimeMillis()
                    ? R.string.reminder_saved_past
                    : !ReminderPreferences.areReminderAlertsEnabled(this)
                    ? R.string.reminder_saved_notifications_disabled
                    : ReminderNotifications.areNotificationsEnabled(this)
                    ? R.string.reminder_saved
                    : R.string.reminder_saved_without_notification_permission;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        } catch (SQLiteException | IllegalArgumentException exception) {
            Toast.makeText(this, R.string.reminder_save_error, Toast.LENGTH_LONG).show();
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.reminder_alarm_error, Toast.LENGTH_LONG).show();
        }
    }
}
