package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.Nullable;

import edu.acg.carsharingapp.R;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLoginDriver, btnLoginPassenger;

    private TextView tvRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLoginDriver = findViewById(R.id.btnLoginDriver);
        btnLoginPassenger = findViewById(R.id.btnLoginPassenger);

        btnLoginDriver.setOnClickListener(v -> loginUser("DRIVER"));
        btnLoginPassenger.setOnClickListener(v -> loginUser("PASSENGER"));

        tvRegister = findViewById(R.id.tvRegister);

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

    }

    private void loginUser(String role) {

        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        String userId = mAuth.getCurrentUser().getUid();

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

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}