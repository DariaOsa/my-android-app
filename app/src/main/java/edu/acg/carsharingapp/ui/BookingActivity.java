package edu.acg.carsharingapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.*;

import java.util.HashMap;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;

public class BookingActivity extends BaseActivity {

    private TextView txtInfo;
    private Button btnAction;
    private Button btnBack;

    private DatabaseReference tripsRef;

    private String tripId;
    private String userId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        txtInfo = findViewById(R.id.txtInfo);
        btnAction = findViewById(R.id.btnJoin);
        btnBack = findViewById(R.id.btnBack);

        tripId = getIntent().getStringExtra("tripId");

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        role = prefs.getString("role", "PASSENGER");

        tripsRef = FirebaseDatabase.getInstance().getReference("trips");

        if (tripId == null) {
            txtInfo.setText("Trip ID missing");
            return;
        }

        loadTrip();

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadTrip() {

        tripsRef.child(tripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            txtInfo.setText("Trip not found");
                            return;
                        }

                        Trip trip = snapshot.getValue(Trip.class);

                        if (trip == null || trip.getStatus() == null) {
                            txtInfo.setText("Error loading trip");
                            return;
                        }

                        txtInfo.setText(
                                "Car: " + trip.getCarName() + "\n" +
                                        "Seats: " + trip.getAvailableSeats() + "\n" +
                                        "Status: " + trip.getStatus()
                        );

                        setupActionButton(trip);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupActionButton(Trip trip) {

        DatabaseReference tripRef = tripsRef.child(tripId);

        boolean isDriver = "DRIVER".equals(role);
        boolean isMine = userId.equals(trip.getDriverId());

        boolean joined =
                trip.getPassengers() != null &&
                        trip.getPassengers().containsKey(userId);

        boolean hasSeats = trip.hasAvailableSeats();

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String activeTrip = prefs.getString("activeTripId", null);

        // ================= DRIVER =================
        if (isDriver) {

            // 🔥 START RIDE
            if (trip.isAvailable() || (trip.isBooked() && isMine)) {

                btnAction.setText("Start Ride");

                btnAction.setOnClickListener(v -> {

                    if (activeTrip != null && !activeTrip.equals(tripId)) {
                        Toast.makeText(this, "Finish your current ride first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    tripRef.child("driverId").setValue(userId);
                    tripRef.child("status").setValue(Trip.STATUS_IN_PROGRESS);

                    prefs.edit().putString("activeTripId", tripId).apply();

                    Toast.makeText(this, "Ride started", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            // 🔥 END RIDE
            else if (trip.isInProgress() && isMine) {

                btnAction.setText("End Ride");

                btnAction.setOnClickListener(v -> {

                    tripRef.child("status").setValue(Trip.STATUS_AVAILABLE);
                    tripRef.child("driverId").setValue(null);
                    tripRef.child("passengers").setValue(null);

                    // 🔥 FIX: reset seats
                    tripRef.child("availableSeats").setValue(3);

                    prefs.edit().remove("activeTripId").apply();

                    Toast.makeText(this, "Ride ended", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            else {
                btnAction.setText("Not Available");
                btnAction.setEnabled(false);
            }

            return;
        }

        // ================= PASSENGER =================

        if (!joined && trip.isInProgress() && hasSeats) {

            btnAction.setText("Join");

            btnAction.setOnClickListener(v -> {

                if (activeTrip != null && !activeTrip.equals(tripId)) {
                    Toast.makeText(this, "Leave current trip first", Toast.LENGTH_SHORT).show();
                    return;
                }

                tripRef.runTransaction(new Transaction.Handler() {

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                        Trip t = currentData.getValue(Trip.class);
                        if (t == null) return Transaction.success(currentData);

                        if (!t.hasAvailableSeats()) {
                            return Transaction.abort();
                        }

                        t.setAvailableSeats(t.getAvailableSeats() - 1);

                        if (t.getPassengers() == null) {
                            t.setPassengers(new HashMap<>());
                        }

                        t.getPassengers().put(userId, true);

                        currentData.setValue(t);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {

                        if (committed) {
                            prefs.edit().putString("activeTripId", tripId).apply();
                            Toast.makeText(BookingActivity.this, "Joined trip", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(BookingActivity.this, "Trip full", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }

        else if (joined) {

            btnAction.setText("Leave");

            btnAction.setOnClickListener(v -> {

                tripRef.runTransaction(new Transaction.Handler() {

                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                        Trip t = currentData.getValue(Trip.class);
                        if (t == null) return Transaction.success(currentData);

                        if (t.getPassengers() != null) {
                            t.getPassengers().remove(userId);
                            t.setAvailableSeats(t.getAvailableSeats() + 1);
                        }

                        currentData.setValue(t);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {

                        if (committed) {
                            prefs.edit().remove("activeTripId").apply();
                            Toast.makeText(BookingActivity.this, "Left trip", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            });
        }

        else {
            btnAction.setText("Not Available");
            btnAction.setEnabled(false);
        }
    }
}

