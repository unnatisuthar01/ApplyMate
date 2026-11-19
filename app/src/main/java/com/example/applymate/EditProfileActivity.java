package com.example.applymate;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.applymate.database.DatabaseHelper;
import com.example.applymate.models.User;
import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private long userId;
    private EditText nameInput;
    private EditText emailInput;
    private EditText mobileInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        ScreenInsets.apply(this);
        userId = ScreenNavigation.requireUserId(this);
        if (userId < 1) {
            return;
        }

        nameInput = findViewById(R.id.input_profile_name);
        emailInput = findViewById(R.id.input_profile_email);
        mobileInput = findViewById(R.id.input_profile_mobile);
        MaterialButton saveButton = findViewById(R.id.button_save_profile);
        saveButton.setOnClickListener(view -> saveProfile());
        loadProfile();
    }

    private void loadProfile() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            User user = database.getUserById(userId);
            if (user == null) {
                finish();
                return;
            }
            nameInput.setText(user.getName());
            emailInput.setText(user.getEmail());
            mobileInput.setText(user.getMobile());
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String mobile = mobileInput.getText().toString().trim();
        boolean valid = true;
        if (name.isEmpty()) {
            nameInput.setError(getString(R.string.form_required));
            valid = false;
        }
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.form_required));
            valid = false;
        } else if (!InputValidator.isValidEmail(email)) {
            emailInput.setError(getString(R.string.invalid_email));
            valid = false;
        }
        if (!mobile.isEmpty() && !InputValidator.isValidMobileNumber(mobile)) {
            mobileInput.setError(getString(R.string.invalid_mobile));
            valid = false;
        }
        if (!valid) {
            return;
        }

        try (DatabaseHelper database = new DatabaseHelper(this)) {
            if (!database.updateUserProfile(userId, name, email, mobile)) {
                emailInput.setError(getString(R.string.email_already_registered));
                return;
            }
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
