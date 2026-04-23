package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.model.Car;
import edu.acg.carsharingapp.data.CarCatalog;

public class BookingActivity extends BaseActivity {

    private TextView txtCar, txtSeats, txtStatus, txtPrice, txtDistance;
    private TextView txtFuel, txtTransmission, txtCategory;

    private MaterialButton btnJoin;
    private ImageView imgCar;

    private DatabaseReference tripRef;
    private String tripId, userId, role;

    private SharedPreferences prefs;

    private LatLng userLocation;
    private LatLng carLocation; // ✅ FIXED (moved to class level)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        setupToolbar();
        initViews();
        loadSession();

        double userLat = getIntent().getDoubleExtra("userLat", 0);
        double userLng = getIntent().getDoubleExtra("userLng", 0);

        double carLat = getIntent().getDoubleExtra("carLat", 0);
        double carLng = getIntent().getDoubleExtra("carLng", 0);

        if (carLat != 0 && carLng != 0) {
            carLocation = new LatLng(carLat, carLng); // ✅ FIXED
        }

        if (userLat != 0 && userLng != 0) {
            userLocation = new LatLng(userLat, userLng); // ✅ FIXED (only once)
        }

        tripId = getIntent().getStringExtra("tripId");

        if (tripId == null || userId == null) {
            Toast.makeText(this, "Error loading trip", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tripRef = FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(tripId);

        loadTrip();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViews() {
        txtCar = findViewById(R.id.txtCar);
        txtSeats = findViewById(R.id.txtSeats);
        txtStatus = findViewById(R.id.txtStatus);
        txtPrice = findViewById(R.id.txtPrice);
        txtDistance = findViewById(R.id.txtDistance);

        txtFuel = findViewById(R.id.txtFuel);
        txtTransmission = findViewById(R.id.txtTransmission);
        txtCategory = findViewById(R.id.txtCategory);

        btnJoin = findViewById(R.id.btnJoin);
        imgCar = findViewById(R.id.imgCar);
        findViewById(R.id.txtRoute).setVisibility(View.GONE);
        findViewById(R.id.txtTime).setVisibility(View.GONE);
    }

    private void loadSession() {
        prefs = getSharedPreferences("session", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        role = prefs.getString("role", "PASSENGER");
    }

    private void loadTrip() {

        tripRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                Car car = CarCatalog.getCarByName(trip.getCarName());
                bindCarUI(car);

                txtSeats.setText(trip.getAvailableSeats() + " seats available");
                double pricePerKm = trip.getPrice();
                txtPrice.setText(String.format("€%.2f / km", pricePerKm));
                txtStatus.setText(trip.getStatus());

                if (userLocation != null) {

                    LatLng carLoc;

                    if (carLocation != null) {
                        carLoc = carLocation; // ✅ FIXED
                    } else {
                        carLoc = new LatLng(
                                trip.getCurrentLat(),
                                trip.getCurrentLng()
                        );
                    }

                    float distance = Math.round(getDistanceKm(userLocation, carLoc) * 10) / 10f;
                    txtDistance.setText(String.format("📏 %.2f km away", distance));
                } else {
                    txtDistance.setText("Distance unavailable");
                }

                handleRoleUI(trip);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindCarUI(Car car) {
        if (car != null) {
            txtCar.setText(car.getDisplayName());
            txtFuel.setText(car.getFuelType());
            txtTransmission.setText(car.getTransmission());
            txtCategory.setText(car.getCategory() + " • ⭐ " + car.getRating());
            imgCar.setImageResource(car.getImageResId());
        }
    }

    private void handleRoleUI(Trip trip) {

        boolean isDriver = "DRIVER".equals(role);

        if (isDriver) {
            handleDriver(trip);
        } else {
            handlePassenger(trip);
        }
    }

    private void handleDriver(Trip trip) {

        if (!trip.isAvailable()) {
            btnJoin.setVisibility(View.GONE);
            return;
        }

        btnJoin.setVisibility(View.VISIBLE);
        btnJoin.setText("Start Ride");

        btnJoin.setOnClickListener(v -> {

            Map<String, Object> updates = new HashMap<>();
            updates.put("driverId", userId);

            if (carLocation != null) {
                updates.put("currentLat", carLocation.latitude);
                updates.put("currentLng", carLocation.longitude);
            }
            tripRef.updateChildren(updates).addOnSuccessListener(unused -> {

                prefs.edit()
                        .putString("activeTripId", tripId)
                        .putBoolean("pickingDestination", true)
                        .apply();

                Toast.makeText(this, "Select destination on map", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(BookingActivity.this, MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        });
    }

    private void handlePassenger(Trip trip) {

        if (!trip.isInProgress()) {
            btnJoin.setVisibility(View.GONE);
            return;
        }

        btnJoin.setVisibility(View.VISIBLE);

        boolean alreadyJoined = trip.hasPassenger(userId);

        if (alreadyJoined) {
            setupLeave();
        } else {
            setupJoin(trip);
        }
    }

    private void setupJoin(Trip trip) {

        if (trip.getAvailableSeats() <= 0) {
            btnJoin.setText("Full");
            btnJoin.setEnabled(false);
            return;
        }

        btnJoin.setText("Join Ride");
        btnJoin.setEnabled(true);

        btnJoin.setOnClickListener(v -> {

            tripRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                    Trip t = currentData.getValue(Trip.class);
                    if (t == null) return Transaction.abort();

                    if (t.getAvailableSeats() <= 0) return Transaction.abort();

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
                        Toast.makeText(BookingActivity.this, "Joined ride!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void setupLeave() {

        btnJoin.setText("Leave Ride");

        btnJoin.setOnClickListener(v -> {

            tripRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                    Trip t = currentData.getValue(Trip.class);
                    if (t == null) return Transaction.abort();

                    if (t.getPassengers() != null) {
                        t.getPassengers().remove(userId);
                    }

                    t.setAvailableSeats(t.getAvailableSeats() + 1);

                    currentData.setValue(t);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {

                    if (committed) {
                        prefs.edit().remove("activeTripId").apply();
                        Toast.makeText(BookingActivity.this, "Left ride", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private float getDistanceKm(LatLng a, LatLng b) {
        float[] results = new float[1];

        android.location.Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                results
        );

        return results[0] / 1000f;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}