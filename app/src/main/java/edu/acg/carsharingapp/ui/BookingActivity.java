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
import com.google.android.material.appbar.MaterialToolbar;
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
    private CheckBox checkTerms;
    private ImageView imgCar;

    private DatabaseReference tripRef;
    private String tripId, userId, role;

    private SharedPreferences prefs;

    private LatLng userLocation;
    private LatLng carLocation;

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
            carLocation = new LatLng(carLat, carLng);
        }

        if (userLat != 0 && userLng != 0) {
            userLocation = new LatLng(userLat, userLng);
        }

        tripId = getIntent().getStringExtra("tripId");

        if (tripId == null || userId == null) {
            Toast.makeText(this, getString(R.string.error_loading_trip), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tripRef = FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(tripId);

        loadTrip();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.app_name));
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
        checkTerms = findViewById(R.id.checkTerms);

        // 🔥 disable button until checkbox is checked
        btnJoin.setEnabled(false);

        checkTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnJoin.setEnabled(isChecked);
        });

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

                txtSeats.setText(getString(R.string.seats_available, trip.getAvailableSeats()));
                txtPrice.setText(getString(R.string.price_per_km, trip.getPrice()));
                txtStatus.setText(getStatusText(trip.getStatus()));

                if (userLocation != null) {

                    LatLng carLoc = (carLocation != null)
                            ? carLocation
                            : new LatLng(trip.getCurrentLat(), trip.getCurrentLng());

                    float distance = Math.round(getDistanceKm(userLocation, carLoc) * 10) / 10f;

                    txtDistance.setText(getString(R.string.distance_km, distance));

                } else {
                    txtDistance.setText(getString(R.string.distance_unavailable));
                }

                handleRoleUI(trip);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindCarUI(Car car) {

        if (car == null) return;

        txtCar.setText(car.getDisplayName());
        txtFuel.setText(getString(car.getFuelTypeResId()));
        txtTransmission.setText(getString(car.getTransmissionResId()));

        txtCategory.setText(
                getString(R.string.car_meta,
                        car.getCategory(),
                        car.getRating())
        );

        imgCar.setImageResource(car.getImageResId());
    }

    private String getStatusText(String status) {

        if (Trip.STATUS_AVAILABLE.equals(status)) {
            return getString(R.string.status_available);
        } else if (Trip.STATUS_IN_PROGRESS.equals(status)) {
            return getString(R.string.status_in_progress);
        } else if (Trip.STATUS_COMPLETED.equals(status)) {
            return getString(R.string.status_completed);
        }

        return status;
    }

    private void handleRoleUI(Trip trip) {

        if ("DRIVER".equals(role)) {
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
        btnJoin.setText(getString(R.string.start_ride));

        btnJoin.setOnClickListener(v -> {

            if (!checkTerms.isChecked()) {
                Toast.makeText(this,
                        getString(R.string.accept_terms_first),
                        Toast.LENGTH_SHORT).show();
                return;
            }

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

                Toast.makeText(this, getString(R.string.select_destination), Toast.LENGTH_SHORT).show();

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

        if (trip.hasPassenger(userId)) {
            setupLeave();
        } else {
            setupJoin(trip);
        }
    }

    private void setupJoin(Trip trip) {

        if (trip.getAvailableSeats() <= 0) {
            btnJoin.setText(getString(R.string.ride_full));
            btnJoin.setEnabled(false);
            return;
        }

        btnJoin.setText(getString(R.string.join_ride));

        btnJoin.setOnClickListener(v -> {

            if (!checkTerms.isChecked()) {
                Toast.makeText(this,
                        getString(R.string.accept_terms_first),
                        Toast.LENGTH_SHORT).show();
                return;
            }

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
                        Toast.makeText(BookingActivity.this,
                                getString(R.string.joined_ride),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void setupLeave() {

        btnJoin.setText(getString(R.string.leave_ride));

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
                        Toast.makeText(BookingActivity.this,
                                getString(R.string.left_ride),
                                Toast.LENGTH_SHORT).show();
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