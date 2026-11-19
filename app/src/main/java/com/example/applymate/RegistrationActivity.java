package com.example.applymate;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.utils.PasswordHasher;

import java.security.GeneralSecurityException;

public class RegistrationActivity extends AppCompatActivity {

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText mobileInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        fullNameInput = findViewById(R.id.input_full_name);
        emailInput = findViewById(R.id.input_email);
        mobileInput = findViewById(R.id.input_mobile);
        passwordInput = findViewById(R.id.input_password);
        confirmPasswordInput = findViewById(R.id.input_confirm_password);

        Button registerButton = findViewById(R.id.button_register);
        TextView loginLink = findViewById(R.id.link_login);
        registerButton.setOnClickListener(view -> attemptRegistration());
        loginLink.setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void attemptRegistration() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String mobile = mobileInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();
        boolean isValid = true;

        if (fullName.isEmpty()) {
            fullNameInput.setError(getString(R.string.form_required));
            isValid = false;
        }

        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidEmail(email)) {
            emailInput.setError(getString(R.string.invalid_email));
            isValid = false;
        }

        if (mobile.isEmpty()) {
            mobileInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidMobileNumber(mobile)) {
            mobileInput.setError(getString(R.string.invalid_mobile));
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidPassword(password)) {
            passwordInput.setError(getString(R.string.invalid_password));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.passwords_mismatch));
            isValid = false;
        }

        if (isValid) {
            try (DatabaseHelper database = new DatabaseHelper(this)) {
                String passwordHash = PasswordHasher.hashAndClear(password.toCharArray());
                long userId = database.insertUser(fullName, email, mobile, passwordHash);
                if (userId == -1L) {
                    emailInput.setError(getString(R.string.email_already_registered));
                    return;
                }
            } catch (GeneralSecurityException exception) {
                Toast.makeText(this, R.string.credential_storage_error, Toast.LENGTH_LONG).show();
                return;
            } catch (SQLiteException exception) {
                Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(loginIntent);
            finish();
        }
    }
}
