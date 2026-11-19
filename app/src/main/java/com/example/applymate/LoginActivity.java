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
import com.example.applymate.models.User;
import com.example.applymate.utils.PasswordHasher;
import com.example.applymate.utils.SessionManager;

import java.security.GeneralSecurityException;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        Button loginButton = findViewById(R.id.button_login);
        TextView forgotPasswordLink = findViewById(R.id.link_forgot_password);
        TextView createAccountLink = findViewById(R.id.link_create_account);

        loginButton.setOnClickListener(view -> attemptLogin());
        forgotPasswordLink.setOnClickListener(view ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        createAccountLink.setOnClickListener(view ->
                startActivity(new Intent(this, RegistrationActivity.class)));
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        boolean isValid = true;

        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidEmail(email)) {
            emailInput.setError(getString(R.string.invalid_email));
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.form_required));
            isValid = false;
        } else if (!InputValidator.isValidPassword(password)) {
            passwordInput.setError(getString(R.string.invalid_password));
            isValid = false;
        }

        if (isValid) {
            try (DatabaseHelper database = new DatabaseHelper(this)) {
                User user = database.findUserByEmail(email);
                if (user == null
                        || !PasswordHasher.verifyAndClear(
                                password.toCharArray(), user.getPasswordHash())) {
                    Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
                    return;
                }

                new SessionManager(this).createSession(user.getId());
                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                finish();
            } catch (GeneralSecurityException exception) {
                Toast.makeText(this, R.string.credential_verification_error, Toast.LENGTH_LONG)
                        .show();
            } catch (SQLiteException exception) {
                Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
