package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import edu.acg.carsharingapp.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends BaseActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ✅ Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 🔹 Initialize fields
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegisterSubmit);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    // =========================
    // 📝 REGISTER LOGIC
    // =========================
    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 🔴 Empty fields
        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 Password match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔴 Password strength (basic)
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔒 Prevent multiple clicks
        btnRegister.setEnabled(false);

        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        // ✅ Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(userId, name, email);

                    } else {

                        btnRegister.setEnabled(true); // 🔥 re-enable

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";

                        Toast.makeText(this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // =========================
    // 💾 SAVE USER DATA
    // =========================
    private void saveUserToDatabase(String userId, String name, String email) {

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("users");

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        dbRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();

                    } else {

                        // 🔥 rollback auth user if DB fails
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete();
                        }

                        btnRegister.setEnabled(true);

                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}