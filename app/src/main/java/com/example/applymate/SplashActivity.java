package com.example.applymate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openWelcomeScreen = () -> {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(openWelcomeScreen, 1200);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openWelcomeScreen);
        super.onDestroy();
    }
}
