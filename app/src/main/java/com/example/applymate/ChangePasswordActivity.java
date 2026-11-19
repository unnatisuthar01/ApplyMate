package com.example.applymate;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.User;
import com.example.applymate.utils.PasswordHasher;
import com.google.android.material.button.MaterialButton;

import java.security.GeneralSecurityException;

public class ChangePasswordActivity extends AppCompatActivity {

    private long userId;
    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        currentPasswordInput = findViewById(R.id.input_change_current_password);
        newPasswordInput = findViewById(R.id.input_change_new_password);
        confirmPasswordInput = findViewById(R.id.input_change_confirm_password);
        MaterialButton saveButton = findViewById(R.id.button_save_password);
        saveButton.setOnClickListener(view -> changePassword());
    }

    private void changePassword() {
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        boolean valid = true;
        if (currentPassword.isEmpty()) {
            currentPasswordInput.setError(getString(R.string.form_required));
            valid = false;
        }
        if (newPassword.isEmpty()) {
            newPasswordInput.setError(getString(R.string.form_required));
            valid = false;
        } else if (!InputValidator.isValidPassword(newPassword)) {
            newPasswordInput.setError(getString(R.string.invalid_password));
            valid = false;
        } else if (currentPassword.equals(newPassword)) {
            newPasswordInput.setError(getString(R.string.password_must_be_different));
            valid = false;
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.form_required));
            valid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.passwords_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        try (DatabaseHelper database = new DatabaseHelper(this)) {
            User user = database.getUserById(userId);
            if (user == null || !PasswordHasher.verifyAndClear(
                    currentPassword.toCharArray(), user.getPasswordHash())) {
                currentPasswordInput.setError(getString(R.string.current_password_incorrect));
                return;
            }
            String newHash = PasswordHasher.hashAndClear(newPassword.toCharArray());
            if (!database.updatePasswordHash(userId, newHash)) {
                Toast.makeText(this, R.string.password_update_error, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (GeneralSecurityException exception) {
            Toast.makeText(this, R.string.credential_storage_error, Toast.LENGTH_LONG).show();
            return;
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show();
        finish();
    }
}
