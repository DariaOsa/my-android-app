package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String role;
    private String userId;

    private DatabaseReference tripsRef;
    private ValueEventListener tripsListener;

    private LinearLayout rideOverlay;
    private TextView txtRideInfo;
    private Button btnEndRide;

    // 🔥 NEW: keep reference so we can control visibility
    private Button btnViewList;

    private String currentRideTripId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        role = prefs.getString("role", "PASSENGER");

        if (userId == null) {
            redirectToLogin();
            return;
        }

        tripsRef = FirebaseDatabase.getInstance().getReference("trips");

        rideOverlay = findViewById(R.id.rideOverlay);
        txtRideInfo = findViewById(R.id.txtRideInfo);
        btnEndRide = findViewById(R.id.btnEndRide);

        // ✅ KEEP ONLY ViewList
        btnViewList = findViewById(R.id.btnViewList);

        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(this, CarListActivity.class))
        );

        btnEndRide.setOnClickListener(v -> {

            if (currentRideTripId == null) {
                Toast.makeText(this, "No active trip!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference tripRef = tripsRef.child(currentRideTripId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Trip.STATUS_AVAILABLE);
            updates.put("driverId", null);
            updates.put("passengers", null);
            updates.put("availableSeats", 3);

            tripRef.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    getSharedPreferences("session", MODE_PRIVATE)
                            .edit().remove("activeTripId").apply();

                    currentRideTripId = null;
                    refreshUI();

                } else {
                    Toast.makeText(this, "Failed to end ride", Toast.LENGTH_SHORT).show();
                }
            });
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!prefs.getBoolean("seeded", false)) {
            seedTestData();
            prefs.edit().putBoolean("seeded", true).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    // =========================
    // ✨ ANIMATIONS
    // =========================
    private void animateRideOverlayIn() {
        rideOverlay.setTranslationY(300);
        rideOverlay.setAlpha(0f);

        rideOverlay.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void animateRideOverlayOut() {
        rideOverlay.animate()
                .translationY(300)
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> rideOverlay.setVisibility(View.GONE))
                .start();
    }

    private void startPulseAnimation(View view) {
        view.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(800)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(800)
                            .withEndAction(() -> startPulseAnimation(view))
                            .start();
                })
                .start();
    }

    private void seedTestData() {

        tripsRef.removeValue();

        Map<String, Object> updates = new HashMap<>();

        for (int i = 0; i < 6; i++) {
            String id = tripsRef.push().getKey();
            Trip t = new Trip(id, null, "Car " + (char)('A' + i), 3);

            if (i < 3) t.setStatus(Trip.STATUS_AVAILABLE);
            else t.setStatus(Trip.STATUS_IN_PROGRESS);

            updates.put(id, t);
        }

        tripsRef.updateChildren(updates);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        refreshUI();
    }

    private void refreshUI() {

        if (mMap == null) return;

        if (tripsListener != null) {
            tripsRef.removeEventListener(tripsListener);
            tripsListener = null;
        }

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String activeTrip = prefs.getString("activeTripId", null);

        if (activeTrip != null) {
            showRideMode(activeTrip);
        } else {
            showBrowsingMode();
        }
    }

    private void showBrowsingMode() {

        animateRideOverlayOut();
        currentRideTripId = null;

        // ✅ SHOW ViewList only here
        btnViewList.setVisibility(View.VISIBLE);

        tripsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                mMap.clear();

                for (DataSnapshot tripSnap : snapshot.getChildren()) {

                    Trip trip = tripSnap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show = false;

                    if ("DRIVER".equals(role)) {
                        if (trip.isAvailable()) show = true;
                    } else {
                        if (trip.isInProgress() && trip.hasAvailableSeats()) show = true;
                    }

                    if (!show) continue;

                    double lat = 37.9838 + (Math.random() - 0.5) * 0.05;
                    double lng = 23.7275 + (Math.random() - 0.5) * 0.05;

                    Marker marker = mMap.addMarker(
                            new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(trip.getCarName() + " | Seats: " + trip.getAvailableSeats())
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    );

                    if (marker != null) {
                        marker.setTag(trip.getTripId());
                    }
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(37.9838, 23.7275), 12
                ));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        tripsRef.addValueEventListener(tripsListener);

        mMap.setOnMarkerClickListener(marker -> {

            if (marker.getTag() == null) return false;

            String tripId = (String) marker.getTag();

            Intent intent = new Intent(MapActivity.this, BookingActivity.class);
            intent.putExtra("tripId", tripId);
            intent.putExtra("userId", userId);
            startActivity(intent);

            return true;
        });
    }

    private void showRideMode(String tripId) {

        currentRideTripId = tripId;

        // ❌ HIDE ViewList during ride
        btnViewList.setVisibility(View.GONE);

        rideOverlay.setVisibility(View.VISIBLE);
        animateRideOverlayIn();
        startPulseAnimation(rideOverlay);

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                mMap.clear();

                LatLng position = new LatLng(37.9838, 23.7275);

                mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("🚗 Your Ride: " + trip.getCarName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14));

                txtRideInfo.setText("🚗 Ride in progress: " + trip.getCarName());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(MapActivity.this, LoginActivity.class));
        finish();
    }
}
