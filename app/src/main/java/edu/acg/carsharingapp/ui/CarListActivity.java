package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
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

        setupToolbar(); // ✅ added

        recyclerView = findViewById(R.id.recyclerCars);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        role = prefs.getString("role", "PASSENGER");
        userId = prefs.getString("userId", null);

        tripsRef = FirebaseDatabase.getInstance().getReference("trips");

        setupSpinner(); // ✅ extracted clean method

        fetchUserLocationAndLoad();
    }

    // =========================
    // 🧭 TOOLBAR
    // =========================

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.app_name)); // ✅ FIXED
        }
    }

    // =========================
    // 🔽 SPINNER
    // =========================

    private void setupSpinner() {
        Spinner spinnerSort = findViewById(R.id.spinnerSort);

        if (spinnerSort == null) return;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sort_options,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        spinnerSort.post(() -> {
            TextView tv = (TextView) spinnerSort.getSelectedView();
            if (tv != null) {
                tv.setTextColor(android.graphics.Color.BLACK);
                tv.setTextSize(16f);
            }
        });

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {

                TextView tv = (TextView) parent.getChildAt(0);
                if (tv != null) {
                    tv.setTextColor(android.graphics.Color.BLACK);
                }

                if (position == 1) {
                    sortMode = "DISTANCE";
                } else if (position == 2) {
                    sortMode = "PRICE";
                } else {
                    return;
                }

                if (userLocation != null) {
                    loadTrips();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
                return;
            }

            loadTrips();
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
                        show = Trip.STATUS_AVAILABLE.equals(trip.getStatus());
                    } else {
                        show = Trip.STATUS_IN_PROGRESS.equals(trip.getStatus())
                                && trip.getDriverId() != null
                                && !trip.getDriverId().equals(userId)
                                && trip.getAvailableSeats() > 0;
                    }

                    if (!show) continue;

                    filteredTrips.add(trip);
                }

                if ("PRICE".equals(sortMode)) {

                    Collections.sort(filteredTrips, (a, b) ->
                            Double.compare(a.getPrice(), b.getPrice()));

                } else if (userLocation != null) {

                    Collections.sort(filteredTrips, (a, b) -> {

                        float distA = getDistanceKm(userLocation,
                                new LatLng(a.getCurrentLat(), a.getCurrentLng()));

                        float distB = getDistanceKm(userLocation,
                                new LatLng(b.getCurrentLat(), b.getCurrentLng()));

                        return Float.compare(distA, distB);
                    });
                }

                CarAdapter adapter = new CarAdapter(
                        filteredTrips,
                        userLocation,
                        trip -> {

                            Intent intent = new Intent(CarListActivity.this, BookingActivity.class);

                            intent.putExtra("tripId", trip.getTripId());

                            if (userLocation != null) {
                                intent.putExtra("userLat", userLocation.latitude);
                                intent.putExtra("userLng", userLocation.longitude);
                            }

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

        if (a == null || b == null) return 0;

        float[] results = new float[1];

        android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                results
        );

        return results[0] / 1000f;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}