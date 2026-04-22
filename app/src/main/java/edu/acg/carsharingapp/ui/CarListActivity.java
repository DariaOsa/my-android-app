package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.adapter.CarAdapter;
import edu.acg.carsharingapp.model.Trip;

public class CarListActivity extends BaseActivity {

    private DatabaseReference tripsRef;
    private RecyclerView recyclerView;

    private String role;
    private String userId;

    private LatLng userLocation;

    private String sortMode = "DISTANCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerCars);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        role = prefs.getString("role", "PASSENGER");
        userId = prefs.getString("userId", null);

        tripsRef = FirebaseDatabase.getInstance().getReference("trips");

        Button btnSortDistance = findViewById(R.id.btnSortDistance);
        Button btnSortPrice = findViewById(R.id.btnSortPrice);

        if (btnSortDistance != null && btnSortPrice != null) {

            btnSortDistance.setOnClickListener(v -> {
                sortMode = "DISTANCE";
                loadTrips();
            });

            btnSortPrice.setOnClickListener(v -> {
                sortMode = "PRICE";
                loadTrips();
            });
        }

        fetchUserLocationAndLoad();
    }

    // =========================
    // 📍 LOCATION
    // =========================

    private void fetchUserLocationAndLoad() {

        var client = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
            );
            return;
        }

        client.getLastLocation().addOnSuccessListener(location -> {

            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                loadTrips();

            } else {
                client.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                ).addOnSuccessListener(loc -> {

                    if (loc != null) {
                        userLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                    } else {
                        userLocation = new LatLng(37.9838, 23.7275);
                    }

                    loadTrips();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001 &&
                grantResults.length > 0 &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            fetchUserLocationAndLoad();
        }
    }

    // =========================
    // 🚗 MAIN LOGIC
    // =========================

    private void loadTrips() {

        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<Trip> filteredTrips = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show;

                    if ("DRIVER".equals(role)) {
                        // Driver sees available cars
                        show = Trip.STATUS_AVAILABLE.equals(trip.getStatus());
                    } else {
                        // Passenger sees active rides (not their own)
                        show = Trip.STATUS_IN_PROGRESS.equals(trip.getStatus())
                                && trip.getDriverId() != null
                                && !trip.getDriverId().equals(userId)
                                && trip.getAvailableSeats() > 0;
                    }

                    if (!show) continue;

                    filteredTrips.add(trip);
                }

                // =========================
                // 🔥 SORTING
                // =========================

                if ("PRICE".equals(sortMode)) {

                    Collections.sort(filteredTrips, (a, b) ->
                            Double.compare(a.getPrice(), b.getPrice()));

                } else {

                    Collections.sort(filteredTrips, (a, b) -> {

                        float distA = getDistanceKm(userLocation,
                                new LatLng(a.getCurrentLat(), a.getCurrentLng()));

                        float distB = getDistanceKm(userLocation,
                                new LatLng(b.getCurrentLat(), b.getCurrentLng()));

                        return Float.compare(distA, distB);
                    });
                }

                // =========================
                // 🔗 ADAPTER
                // =========================

                CarAdapter adapter = new CarAdapter(
                        filteredTrips,
                        userLocation,
                        trip -> {

                            Intent intent = new Intent(CarListActivity.this, BookingActivity.class);

                            intent.putExtra("tripId", trip.getTripId());
                            intent.putExtra("userLat", userLocation.latitude);
                            intent.putExtra("userLng", userLocation.longitude);

                            startActivity(intent);
                        }
                );

                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // =========================
    // 📏 DISTANCE
    // =========================

    private float getDistanceKm(LatLng a, LatLng b) {

        float[] results = new float[1];

        android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                results
        );

        return results[0] / 1000f;
    }

    // 🔙 Back
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}