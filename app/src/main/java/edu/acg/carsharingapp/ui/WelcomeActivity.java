package edu.acg.carsharingapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import edu.acg.carsharingapp.R;
import android.content.Intent;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 🔘 Buttons
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnMap = findViewById(R.id.btnMap); // NEW BUTTON

        // 🔐 Login
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        // 📝 Register
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });

        // 🗺️ Open Map
        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, MapActivity.class));
        });
    }
}