package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import edu.acg.carsharingapp.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegisterSubmit);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        Log.d(TAG, "Attempting registration with email: [" + email + "]");

        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword)) {

            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        Toast.makeText(this, getString(R.string.creating_account), Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        if (mAuth.getCurrentUser() == null) {
                            Log.e(TAG, "Auth succeeded but user is null!");
                            btnRegister.setEnabled(true);
                            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToDatabase(userId, name, email);

                    } else {

                        btnRegister.setEnabled(true);

                        Exception e = task.getException();
                        Log.e(TAG, "Registration failed", e);

                        String message = getString(R.string.registration_failed);

                        if (e instanceof FirebaseAuthException) {

                            String code = ((FirebaseAuthException) e).getErrorCode();

                            switch (code) {
                                case "ERROR_EMAIL_ALREADY_IN_USE":
                                    message = getString(R.string.email_in_use);
                                    break;

                                case "ERROR_INVALID_EMAIL":
                                    message = getString(R.string.invalid_email_format);
                                    break;

                                case "ERROR_WEAK_PASSWORD":
                                    message = getString(R.string.weak_password);
                                    break;

                                case "ERROR_NETWORK_REQUEST_FAILED":
                                    message = getString(R.string.network_error);
                                    break;

                                case "ERROR_TOO_MANY_REQUESTS":
                                    message = getString(R.string.too_many_requests);
                                    break;

                                default:
                                    message = getString(R.string.auth_error, code);
                                    break;
                            }

                        } else if (e != null) {
                            message = e.getMessage();
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

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

                        Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();

                    } else {

                        Log.e(TAG, "Database write failed", task.getException());

                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete()
                                    .addOnFailureListener(err ->
                                            Log.e(TAG, "Rollback delete failed", err));
                        }

                        btnRegister.setEnabled(true);

                        Toast.makeText(this, getString(R.string.save_user_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}