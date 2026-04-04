package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import edu.acg.carsharingapp.R;

public class WelcomeActivity extends BaseActivity {

    private Button btnFindCar, btnLogin, btnRegister;

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

        // 🌍 Language switch
        ImageView imgEnglish = findViewById(R.id.imgEnglish);
        ImageView imgGreek = findViewById(R.id.imgGreek);

        imgEnglish.setOnClickListener(v -> setLanguage("en"));
        imgGreek.setOnClickListener(v -> setLanguage("el"));
    }

    private void setLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);
        recreate();
    }
}