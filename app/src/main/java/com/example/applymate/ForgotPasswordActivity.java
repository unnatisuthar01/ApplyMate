package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.User;
import com.example.applymate.utils.PasswordHasher;
import com.example.applymate.utils.SessionManager;

import java.security.GeneralSecurityException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        ScreenInsets.apply(this);

        emailInput = findViewById(R.id.input_email);
        currentPasswordInput = findViewById(R.id.input_current_password);
        newPasswordInput = findViewById(R.id.input_new_password);
        confirmPasswordInput = findViewById(R.id.input_confirm_new_password);

        Button resetButton = findViewById(R.id.button_reset_password);
        resetButton.setOnClickListener(view -> attemptPasswordReset());
    }

    private void attemptPasswordReset() {
        String email = emailInput.getText().toString().trim();
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        boolean isValid = true;

        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidEmail(email)) {
            emailInput.setError(getString(R.string.invalid_email));
            isValid = false;
        }

        if (currentPassword.isEmpty()) {
            currentPasswordInput.setError(getString(R.string.form_required));
            isValid = false;
        }

        if (newPassword.isEmpty()) {
            newPasswordInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidPassword(newPassword)) {
            newPasswordInput.setError(getString(R.string.invalid_password));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.passwords_mismatch));
            isValid = false;
        }

        if (isValid) {
            try (DatabaseHelper database = new DatabaseHelper(this)) {
                User user = database.findUserByEmail(email);
                if (user == null || !PasswordHasher.verifyAndClear(
                        currentPassword.toCharArray(), user.getPasswordHash())) {
                    currentPasswordInput.setError(getString(R.string.current_password_incorrect));
                    return;
                }

                String newPasswordHash =
                        PasswordHasher.hashAndClear(newPassword.toCharArray());
                if (!database.updatePasswordHash(user.getId(), newPasswordHash)) {
                    Toast.makeText(this, R.string.password_update_error, Toast.LENGTH_LONG).show();
                    return;
                }

                SessionManager sessionManager = new SessionManager(this);
                if (sessionManager.getCurrentUserId() == user.getId()) {
                    sessionManager.clearSession();
                }
            } catch (GeneralSecurityException exception) {
                Toast.makeText(this, R.string.credential_storage_error, Toast.LENGTH_LONG).show();
                return;
            } catch (SQLiteException exception) {
                Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, R.string.password_updated, Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        }
    }
}
