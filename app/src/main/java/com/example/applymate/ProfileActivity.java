package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.Reminder;
import com.example.applymate.models.User;
import com.example.applymate.utils.SessionManager;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private long userId;
    private TextView nameView;
    private TextView emailView;
    private TextView mobileView;
    private TextView accountIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        nameView = findViewById(R.id.text_profile_name);
        emailView = findViewById(R.id.text_profile_email);
        mobileView = findViewById(R.id.text_profile_mobile);
        accountIdView = findViewById(R.id.text_profile_account_id);
        findViewById(R.id.button_edit_profile).setOnClickListener(view ->
                startActivity(new Intent(this, EditProfileActivity.class)));
        findViewById(R.id.button_change_password).setOnClickListener(view ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.button_profile_settings).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.button_profile_logout).setOnClickListener(view ->
                showLogoutConfirmation());
        findViewById(R.id.button_delete_account).setOnClickListener(view ->
                showDeleteAccountConfirmation());
        ScreenNavigation.configureBottomNavigation(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId > 0) {
            loadProfile();
        }
    }

    private void loadProfile() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            User user = database.getUserById(userId);
            if (user == null) {
                new SessionManager(this).clearSession();
                navigateToWelcome();
                return;
            }
            nameView.setText(user.getName());
            emailView.setText(user.getEmail());
            mobileView.setText(user.getMobile() == null || user.getMobile().trim().isEmpty()
                    ? getString(R.string.unknown_value) : user.getMobile());
            accountIdView.setText(getString(R.string.account_id, user.getId()));
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.logout_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    new SessionManager(this).clearSession();
                    navigateToWelcome();
                })
                .show();
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_account, (dialog, which) ->
                        deleteAccount())
                .show();
    }

    private void deleteAccount() {
        List<Reminder> reminders;
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            reminders = database.getRemindersForUser(userId, null);
            if (!database.deleteUserAccount(userId)) {
                Toast.makeText(this, R.string.account_delete_error, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.account_delete_error, Toast.LENGTH_LONG).show();
            return;
        }

        for (Reminder reminder : reminders) {
            ReminderScheduler.cancel(this, reminder.getId());
            ReminderNotifications.cancel(this, reminder.getId());
        }
        new SessionManager(this).clearSession();
        Toast.makeText(this, R.string.account_deleted, Toast.LENGTH_SHORT).show();
        navigateToWelcome();
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
