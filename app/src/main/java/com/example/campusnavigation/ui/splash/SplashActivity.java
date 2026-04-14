package com.example.campusnavigation.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusnavigation.CampusNavigationApp;
import com.example.campusnavigation.R;
import com.example.campusnavigation.ui.auth.AuthActivity;
import com.example.campusnavigation.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = CampusNavigationApp.getInstance().getAuthRepository().isLoggedIn()
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        }, 1400);
    }
}
