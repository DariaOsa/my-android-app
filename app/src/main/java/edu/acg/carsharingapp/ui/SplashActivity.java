package edu.acg.carsharingapp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import edu.acg.carsharingapp.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.loadLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
            String userId = prefs.getString("userId", null);

            Intent intent;

            if (userId != null && !userId.isEmpty()) {
                // ✅ already logged in → go to map
                intent = new Intent(SplashActivity.this, MapActivity.class);
            } else {
                // ❌ not logged in → go to welcome
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            finish();

        }, 1500);
    }
}