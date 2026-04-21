package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.HashMap;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.model.Car;
import edu.acg.carsharingapp.data.CarRepository;

public class BookingActivity extends BaseActivity {

    private TextView txtDistance, txtCar, txtSeats, txtStatus;
    private TextView txtRoute, txtTime, txtPrice;
    private TextView txtFuel, txtTransmission, txtCategory;

    private MaterialButton btnJoin;
    private ImageView imgCar;

    private DatabaseReference tripRef;
    private String tripId;
    private String userId;
    private String role;

    private SharedPreferences prefs;

    // ✅ NEW: user location from Map/List
    private LatLng userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        setupToolbar();
        initViews();
        loadSession();

        // ✅ RECEIVE USER LOCATION (NEW WAY)
        double userLat = getIntent().getDoubleExtra("userLat", 0);
        double userLng = getIntent().getDoubleExtra("userLng", 0);

        if (userLat != 0 && userLng != 0) {
            userLocation = new LatLng(userLat, userLng);
        }

        // ✅ ONLY SOURCE OF TRUTH
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
            getSupportActionBar().setTitle("CarSharingApp");
        }
    }

    private void initViews() {
        txtCar = findViewById(R.id.txtCar);
        txtSeats = findViewById(R.id.txtSeats);
        txtStatus = findViewById(R.id.txtStatus);
        txtRoute = findViewById(R.id.txtRoute);
        txtTime = findViewById(R.id.txtTime);
        txtPrice = findViewById(R.id.txtPrice);
        txtDistance = findViewById(R.id.txtDistance);

        txtFuel = findViewById(R.id.txtFuel);
        txtTransmission = findViewById(R.id.txtTransmission);
        txtCategory = findViewById(R.id.txtCategory);

        btnJoin = findViewById(R.id.btnJoin);
        imgCar = findViewById(R.id.imgCar);
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

                Car car = CarRepository.getCarByName(trip.getCarName());
                bindCarUI(car);

                txtSeats.setText(trip.getAvailableSeats() + " seats available");
                txtPrice.setText(trip.getFormattedPrice());

                // ✅ CALCULATE DISTANCE HERE (single source of truth)
                if (userLocation != null) {
                    LatLng carLocation = new LatLng(trip.getFromLat(), trip.getFromLng());
                    float distance = getDistanceKm(userLocation, carLocation);
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
        } else {
            txtCar.setText("Unknown Car");
            imgCar.setImageResource(R.drawable.car1);
        }
    }

    private void setStatus(String text, int color) {
        txtStatus.setText(text);
        txtStatus.setTextColor(color);
    }

    private void handleRoleUI(Trip trip) {
        boolean isDriver = "DRIVER".equals(role);

        if (isDriver) {
            handleDriverUI(trip);
        } else {
            handlePassengerUI(trip);
        }
    }

    private void handleDriverUI(Trip trip) {

        txtRoute.setVisibility(View.GONE);
        txtTime.setVisibility(View.GONE);

        btnJoin.setVisibility(View.VISIBLE);

        if (trip.isAvailable()) {

            setStatus("READY", 0xFF4CAF50);
            btnJoin.setText("Start Ride");

            btnJoin.setOnClickListener(v -> {
                tripRef.child("driverId").setValue(userId);
                prefs.edit()
                        .putString("activeTripId", tripId)
                        .putBoolean("pickingDestination", true)
                        .apply();

                Toast.makeText(this, "Select destination on map", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, MapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });

        } else {
            btnJoin.setVisibility(View.GONE);
        }
    }

    private void handlePassengerUI(Trip trip) {

        txtRoute.setVisibility(View.VISIBLE);
        txtTime.setVisibility(View.VISIBLE);

        txtRoute.setText("📍 " + trip.getRoute());
        txtTime.setText("⏰ " + trip.getDepartureTime());

        if (!trip.isInProgress()) {
            btnJoin.setVisibility(View.GONE);
            return;
        }

        btnJoin.setVisibility(View.VISIBLE);

        boolean alreadyJoined = trip.hasPassenger(userId);

        if (alreadyJoined) {
            setupLeaveRide(trip);
        } else {
            setupJoinRide(trip);
        }
    }

    private void setupLeaveRide(Trip trip) {

        btnJoin.setText("Leave Ride");

        btnJoin.setOnClickListener(v -> {

            trip.getPassengers().remove(userId);
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);

            tripRef.setValue(trip);

            prefs.edit().remove("activeTripId").apply();

            Toast.makeText(this, "Left ride", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupJoinRide(Trip trip) {

        if (!trip.hasAvailableSeats()) {
            btnJoin.setText("Full");
            btnJoin.setEnabled(false);
            return;
        }

        btnJoin.setEnabled(true);
        btnJoin.setText("Join Ride");

        btnJoin.setOnClickListener(v -> {

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
                public void onComplete(DatabaseError error,
                                       boolean committed,
                                       DataSnapshot snapshot) {

                    if (committed) {
                        prefs.edit()
                                .putString("activeTripId", tripId)
                                .apply();

                        Toast.makeText(BookingActivity.this,
                                "Joined ride!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    // ✅ distance helper
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