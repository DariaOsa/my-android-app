package edu.acg.carsharingapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.acg.carsharingapp.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.loadLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 🔙 Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 🔍 Find views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLoginSubmit);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 📧 Email validation
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        // 🔒 Password validation
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_length));
            etPassword.requestFocus();
            return;
        }

        // 📦 Get saved credentials
        String savedEmail = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("email", null);

        String savedPassword = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getString("password", null);

        // 🚫 No account exists
        if (savedEmail == null || savedPassword == null) {
            Toast.makeText(this, getString(R.string.error_no_account), Toast.LENGTH_SHORT).show();
            return;
        }

        // ❌ Wrong credentials
        if (!email.equals(savedEmail) || !password.equals(savedPassword)) {
            Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Success
        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

        startActivity(new Intent(LoginActivity.this, MapActivity.class));
        finish();
    }

    // 🔙 Handle toolbar back
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}