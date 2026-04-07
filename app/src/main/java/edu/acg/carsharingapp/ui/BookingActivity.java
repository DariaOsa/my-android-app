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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;

import java.util.HashMap;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.model.Car;
import edu.acg.carsharingapp.data.CarRepository;

public class BookingActivity extends BaseActivity {

    // 🔤 TEXT VIEWS
    private TextView txtCar, txtSeats, txtStatus;
    private TextView txtRoute, txtTime, txtPrice;
    private TextView txtFuel, txtTransmission, txtCategory;

    // 🔘 UI
    private MaterialButton btnJoin;
    private ImageView imgCar;

    // 🔥 DATA
    private DatabaseReference tripRef;
    private String tripId;
    private String userId;
    private String role;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        setupToolbar();
        initViews();
        loadSession();

        // =========================
        // 🚗 FLOW 1: FROM CAR LIST
        // =========================
        Car car = (Car) getIntent().getSerializableExtra("car");

        if (car != null) {
            showCarPreview(car);
            return;
        }

        // =========================
        // 🚗 FLOW 2: FROM MAP
        // =========================
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

    // =========================
    // 🔧 SETUP METHODS
    // =========================

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

    // =========================
    // 🚗 CAR PREVIEW MODE
    // =========================

    private void showCarPreview(Car car) {
        bindCarUI(car);

        txtSeats.setText(car.getSeats() + " seats");
        txtPrice.setText(car.getFormattedPrice());

        txtRoute.setVisibility(View.GONE);
        txtTime.setVisibility(View.GONE);

        setStatus("READY", 0xFF4CAF50);

        btnJoin.setText("Start Ride");
        btnJoin.setOnClickListener(v ->
                Toast.makeText(this, "Create trip logic later", Toast.LENGTH_SHORT).show()
        );
    }

    // =========================
    // 🔥 LOAD TRIP
    // =========================

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

                handleRoleUI(trip);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // =========================
    // 🎨 UI HELPERS
    // =========================

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

    // =========================
    // 👤 ROLE HANDLING
    // =========================

    private void handleRoleUI(Trip trip) {

        boolean isDriver = "DRIVER".equals(role);

        if (isDriver) {
            handleDriverUI(trip);
        } else {
            handlePassengerUI(trip);
        }
    }

    // =========================
    // 🚗 DRIVER MODE
    // =========================

    private void handleDriverUI(Trip trip) {

        txtRoute.setVisibility(View.GONE);
        txtTime.setVisibility(View.GONE);

        btnJoin.setVisibility(View.VISIBLE);

        if (trip.isAvailable()) {

            setStatus("READY", 0xFF4CAF50);
            btnJoin.setText("Start Ride");

            btnJoin.setOnClickListener(v -> {

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

    // =========================
    // 🧍 PASSENGER MODE
    // =========================

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

    // =========================
    // 🔙 BACK BUTTON
    // =========================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}