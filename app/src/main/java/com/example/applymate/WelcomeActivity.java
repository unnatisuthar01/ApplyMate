package com.example.applymate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ScreenInsets.apply(this);

        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);
        loginButton.setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));
        registerButton.setOnClickListener(view ->
                startActivity(new Intent(this, RegistrationActivity.class)));
    }
}
