package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.adapter.SimpleHistoryAdapter;

public class ProfileActivity extends BaseActivity {

    private TextView txtName, txtEmail;
    private Button btnLogout;
    private RecyclerView recyclerHistory;
    private CheckBox checkDarkMode;

    private String userId;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupToolbar();

        // =========================
        // 🧩 INIT VIEWS
        // =========================
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        checkDarkMode = findViewById(R.id.checkDarkMode);

        // =========================
        // 💾 PREFS (ONLY ONCE)
        // =========================
        prefs = getSharedPreferences("session", MODE_PRIVATE);

        // ✅ checkbox state
        boolean isDark = prefs.getBoolean("darkMode", false);
        checkDarkMode.setChecked(isDark);

        checkDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {

            prefs.edit().putBoolean("darkMode", isChecked).apply();

            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }

            // 🔥 THIS LINE makes it instant
            recreate();
        });

        // =========================
        // 👤 USER
        // =========================
        userId = prefs.getString("userId", null);

        if (userId == null) {
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    txtName.setText(name != null ? name : getString(R.string.no_name));
                    txtEmail.setText(email != null ? email : getString(R.string.no_email));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.toException().printStackTrace();
            }
        });

        // =========================
        // 📜 HISTORY
        // =========================
        DatabaseReference historyRef =
                FirebaseDatabase.getInstance()
                        .getReference("history")
                        .child(userId);

        List<Trip> historyList = new ArrayList<>();
        SimpleHistoryAdapter adapter = new SimpleHistoryAdapter(historyList);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                historyList.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    historyList.add(trip);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.toException().printStackTrace();
            }
        });

        // =========================
        // 🚪 LOGOUT
        // =========================
        btnLogout.setText(getString(R.string.logout));

        btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            prefs.edit().clear().apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });
    }

    // =========================
    // 🔝 TOOLBAR
    // =========================
    private void setupToolbar() {

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // 🔙 BACK BUTTON
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}