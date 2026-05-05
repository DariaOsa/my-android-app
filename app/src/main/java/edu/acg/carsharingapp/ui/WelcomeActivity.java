package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import edu.acg.carsharingapp.R;

public class WelcomeActivity extends BaseActivity {

    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 SESSION CHECK (ENTRY GATE)
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);

        String userId = prefs.getString("userId", null);
        String role = prefs.getString("role", null);

        if (userId != null && role != null) {
            startActivity(new Intent(this, MapActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        // 🔘 Buttons
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // 🔐 Login
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        // 📝 Register
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class))
        );

        // 🌍 Language switch
        ImageView imgEnglish = findViewById(R.id.imgEnglish);
        ImageView imgGreek = findViewById(R.id.imgGreek);

        imgEnglish.setOnClickListener(v -> setLanguage("en"));
        imgGreek.setOnClickListener(v -> setLanguage("el"));
    }

    // 🌍 Apply language globally (FULL restart)
    private void setLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}