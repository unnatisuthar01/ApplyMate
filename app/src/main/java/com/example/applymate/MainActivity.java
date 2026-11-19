package com.example.applymate;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.applymate.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);
        Button logoutButton = findViewById(R.id.button_logout);
        SessionManager sessionManager = new SessionManager(this);

        boolean isLoggedIn = sessionManager.isLoggedIn();
        loginButton.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        registerButton.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        logoutButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        loginButton.setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));
        registerButton.setOnClickListener(view ->
                startActivity(new Intent(this, RegistrationActivity.class)));
        logoutButton.setOnClickListener(view -> {
            sessionManager.clearSession();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        });
    }
}