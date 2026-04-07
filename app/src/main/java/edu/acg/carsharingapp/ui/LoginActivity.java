package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.DatabaseHelper;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLoginDriver, btnLoginPassenger, btnRegister;

    private DatabaseHelper db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);

        // ✅ Auto-login if session exists
        String existingUser = prefs.getString("userId", null);
        String existingRole = prefs.getString("role", null);

        if (existingUser != null && existingRole != null) {
            startActivity(new Intent(this, MapActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLoginDriver = findViewById(R.id.btnLoginDriver);
        btnLoginPassenger = findViewById(R.id.btnLoginPassenger);
        btnRegister = findViewById(R.id.btnRegister);

        btnLoginDriver.setOnClickListener(v -> loginUser("DRIVER"));
        btnLoginPassenger.setOnClickListener(v -> loginUser("PASSENGER"));

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void loginUser(String role) {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = null;

        try {
            cursor = db.getUser(email, password);

            if (cursor != null && cursor.moveToFirst()) {

                // ✅ SAFER column access
                int userIdIndex = cursor.getColumnIndex("id"); // <-- CHANGE if your column name differs
                String userId;

                if (userIdIndex != -1) {
                    userId = cursor.getString(userIdIndex);
                } else {
                    // fallback (not ideal but prevents crash)
                    userId = cursor.getString(0);
                }

                // ✅ Save session
                SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                prefs.edit()
                        .putString("userId", userId)
                        .putString("role", role)
                        .apply();

                Toast.makeText(this, "Login as " + role, Toast.LENGTH_SHORT).show();

                startActivity(new Intent(LoginActivity.this, MapActivity.class));
                finish();

            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}