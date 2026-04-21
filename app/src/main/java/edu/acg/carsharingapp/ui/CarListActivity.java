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
import edu.acg.carsharingapp.data.CarRepository;
import edu.acg.carsharingapp.model.Car;
import edu.acg.carsharingapp.model.Trip;

public class CarListActivity extends BaseActivity {

    private DatabaseReference tripsRef;
    private RecyclerView recyclerView;
    private String role;

    private LatLng userLocation;

    // ✅ NEW: sorting mode
    private String sortMode = "DISTANCE"; // default

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

        tripsRef = FirebaseDatabase.getInstance().getReference("trips");

        // ✅ OPTIONAL: hook buttons if you added them in XML
        Button btnSortDistance = findViewById(R.id.btnSortDistance);
        Button btnSortPrice = findViewById(R.id.btnSortPrice);
        // ✅ Default = DISTANCE selected on screen load
        btnSortDistance.setTextColor(getColor(R.color.blue));
        btnSortPrice.setTextColor(getColor(R.color.gray));

// Optional but recommended (match background too)
        btnSortDistance.setBackgroundResource(R.drawable.bg_sort_button_selected);
        btnSortPrice.setBackgroundResource(R.drawable.bg_sort_button);

        if (btnSortDistance != null) {
            btnSortDistance.setOnClickListener(v -> {
                sortMode = "DISTANCE";
                btnSortDistance.setBackgroundResource(R.drawable.bg_sort_button_selected);
                btnSortPrice.setBackgroundResource(R.drawable.bg_sort_button);
                btnSortDistance.setTextColor(getColor(R.color.blue));   // active
                btnSortPrice.setTextColor(getColor(R.color.gray));

                loadCars();
            });
        }

        if (btnSortPrice != null) {
            btnSortPrice.setOnClickListener(v -> {
                sortMode = "PRICE";
                btnSortDistance.setBackgroundResource(R.drawable.bg_sort_button_selected);
                btnSortPrice.setBackgroundResource(R.drawable.bg_sort_button);
                btnSortDistance.setTextColor(getColor(R.color.gray));   // inactive
                btnSortPrice.setTextColor(getColor(R.color.blue));

                loadCars();
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
                userLocation = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );
                loadCars();

            } else {
                client.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                ).addOnSuccessListener(loc -> {

                    if (loc != null) {
                        userLocation = new LatLng(
                                loc.getLatitude(),
                                loc.getLongitude()
                        );
                    } else {
                        userLocation = new LatLng(37.9838, 23.7275);
                    }

                    loadCars();
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

    private void loadCars() {

        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                List<Car> filteredCars = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show;

                    if ("DRIVER".equals(role)) {
                        show = Trip.STATUS_AVAILABLE.equals(trip.getStatus());
                    } else {
                        show = Trip.STATUS_IN_PROGRESS.equals(trip.getStatus())
                                && trip.hasAvailableSeats();
                    }

                    if (!show) continue;

                    Car baseCar = CarRepository.getCarByName(trip.getCarName());
                    if (baseCar == null) continue;

                    Car car = new Car(
                            baseCar.getBrand(),
                            baseCar.getModel(),
                            baseCar.getCategory(),
                            baseCar.getPricePerTrip(),
                            baseCar.getRating(),
                            baseCar.getImageResId(),
                            baseCar.getSeats(),
                            baseCar.getFuelType(),
                            baseCar.getTransmission()
                    );

                    car.setTripId(snap.getKey());
                    car.setLatitude(trip.getFromLat());
                    car.setLongitude(trip.getFromLng());

                    // ✅ NEW: calculate distance ONCE
                    if (userLocation != null) {
                        LatLng carLoc = new LatLng(car.getLatitude(), car.getLongitude());
                        float distance = getDistanceKm(userLocation, carLoc);
                        car.setDistanceKm(distance);
                    }

                    filteredCars.add(car);
                }

                // =========================
                // 🔥 SORTING
                // =========================

                if ("PRICE".equals(sortMode)) {
                    Collections.sort(filteredCars, (a, b) ->
                            Double.compare(a.getPricePerTrip(), b.getPricePerTrip()));
                } else {
                    Collections.sort(filteredCars, (a, b) ->
                            Float.compare(a.getDistanceKm(), b.getDistanceKm()));
                }

                CarAdapter adapter = new CarAdapter(filteredCars, userLocation, car -> {

                    Intent intent = new Intent(CarListActivity.this, BookingActivity.class);

                    intent.putExtra("tripId", car.getTripId());
                    intent.putExtra("userLat", userLocation.latitude);
                    intent.putExtra("userLng", userLocation.longitude);

                    startActivity(intent);
                });

                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // =========================
    // 📏 DISTANCE HELPER
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