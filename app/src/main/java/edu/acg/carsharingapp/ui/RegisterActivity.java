package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.DatabaseHelper;

public class RegisterActivity extends BaseActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DatabaseHelper(this);

        // 🔹 Initialize fields
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // ✅ FIXED BUTTON ID (matches XML)
        btnRegister = findViewById(R.id.btnRegisterSubmit);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    // =========================
    // 📝 REGISTER LOGIC
    // =========================
    private void registerUser() {
        try {

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 🔴 Empty fields validation
            if (TextUtils.isEmpty(name) ||
                    TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(password) ||
                    TextUtils.isEmpty(confirmPassword)) {

                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔴 Password match validation
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Checking email...", Toast.LENGTH_SHORT).show();

            // 🔴 Check existing email
            if (db.emailExists(email)) {
                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Inserting user...", Toast.LENGTH_SHORT).show();

            // ✅ Create user
            String userId = UUID.randomUUID().toString();

            boolean success = db.insertUser(userId, name, email, password);

            if (success) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();

                // 👉 Go to login
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "CRASH: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}