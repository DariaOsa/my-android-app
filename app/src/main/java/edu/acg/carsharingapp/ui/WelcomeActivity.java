package edu.acg.carsharingapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import edu.acg.carsharingapp.R;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnFindCar, btnLogin, btnRegister;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.loadLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnFindCar = findViewById(R.id.btnFindCar);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // 🚗 Open Map
        btnFindCar.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, MapActivity.class))
        );

        // 🔐 Login
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        // 📝 Register
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class))
        );
    }
}