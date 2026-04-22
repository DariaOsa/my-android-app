package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtName, txtEmail;
    private Button btnLogout;
    private RecyclerView recyclerHistory;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ✅ Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ✅ Bind views
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerHistory = findViewById(R.id.recyclerHistory);

        // ✅ Get session user
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        if (userId == null) {
            // Safety fallback
            finish();
            return;
        }

        // =========================
        // ✅ LOAD USER FROM FIREBASE
        // =========================
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    txtName.setText(name != null ? name : "No name");
                    txtEmail.setText(email != null ? email : "No email");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.toException().printStackTrace();
            }
        });

        // =========================
        // ✅ FIREBASE HISTORY
        // =========================

        DatabaseReference historyRef =
                FirebaseDatabase.getInstance()
                        .getReference("history")
                        .child(userId);

        List<String> historyList = new ArrayList<>();
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

                    historyList.add(trip.getHistoryText());
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                error.toException().printStackTrace();
            }
        });

        // =========================
        // ✅ LOGOUT
        // =========================

        btnLogout.setOnClickListener(v -> {

            // 🔥 Firebase logout
            FirebaseAuth.getInstance().signOut();

            // 🔥 Clear session
            getSharedPreferences("session", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });
    }

    // ✅ Back arrow
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}