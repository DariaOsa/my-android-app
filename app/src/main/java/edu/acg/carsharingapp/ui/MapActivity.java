package edu.acg.carsharingapp.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import java.util.*;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.data.CarRepository;
import edu.acg.carsharingapp.model.Car;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String role;
    private String userId;

    private DatabaseReference tripsRef;
    private ValueEventListener tripsListener;

    private LinearLayout rideOverlay;
    private TextView txtRideInfo;
    private Button btnEndRide;
    private Button btnViewList;

    private String currentRideTripId = null;

    private Marker carMarker;
    private ValueAnimator carAnimator;

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
        btnViewList = findViewById(R.id.btnViewList);

        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(this, CarListActivity.class))
        );

        btnEndRide.setOnClickListener(v -> {

            if (currentRideTripId == null) {
                Toast.makeText(this, "No active trip!", Toast.LENGTH_SHORT).show();
                return;
            }

            Car car = CarRepository.getCars().get(0);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Trip.STATUS_AVAILABLE);
            updates.put("driverId", null);
            updates.put("passengers", null);
            updates.put("availableSeats", car.getSeats());

            tripsRef.child(currentRideTripId).updateChildren(updates);

            getSharedPreferences("session", MODE_PRIVATE)
                    .edit()
                    .remove("activeTripId")
                    .remove("pickingDestination")
                    .apply();

            currentRideTripId = null;
            stopCarAnimation();
            refreshUI();
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

    private void seedTestData() {

        tripsRef.removeValue();
        Map<String, Object> updates = new HashMap<>();

        for (int i = 0; i < 6; i++) {

            String id = tripsRef.push().getKey();
            Car car = CarRepository.getCars().get(i % CarRepository.getCars().size());

            double lat = 37.9838 + (Math.random() - 0.5) * 0.05;
            double lng = 23.7275 + (Math.random() - 0.5) * 0.05;

            Trip t = new Trip(
                    id,
                    null,
                    car.getDisplayName(),
                    car.getPricePerTrip(),
                    "Athens",
                    "Piraeus",
                    "Today • " + (14 + i) + ":00",
                    car.getSeats()
            );

            t.setFromLat(lat);
            t.setFromLng(lng);

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
        }

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String activeTrip = prefs.getString("activeTripId", null);
        boolean pickingDestination = prefs.getBoolean("pickingDestination", false);

        if (activeTrip != null && pickingDestination) {
            enableDestinationPicking(activeTrip);
        } else if (activeTrip != null) {
            showRideMode(activeTrip);
        } else {
            showBrowsingMode();
        }
    }

    private void showBrowsingMode() {

        rideOverlay.setVisibility(View.GONE);
        currentRideTripId = null;
        btnViewList.setVisibility(View.VISIBLE);

        tripsListener = tripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mMap == null) return;

                mMap.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show = "DRIVER".equals(role)
                            ? trip.isAvailable()
                            : trip.isInProgress() && trip.hasAvailableSeats();

                    if (!show) continue;

                    LatLng pos = new LatLng(trip.getFromLat(), trip.getFromLng());

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(trip.getCarName()));

                    if (m != null) m.setTag(trip.getTripId());
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(37.9838, 23.7275), 12));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mMap.setOnMarkerClickListener(marker -> {
            Intent i = new Intent(this, BookingActivity.class);
            i.putExtra("tripId", (String) marker.getTag());
            startActivity(i);
            return true;
        });
    }

    private void enableDestinationPicking(String tripId) {

        if (tripsListener != null) tripsRef.removeEventListener(tripsListener);

        mMap.clear();

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                LatLng car = new LatLng(trip.getFromLat(), trip.getFromLng());

                mMap.addMarker(new MarkerOptions().position(car).title("Your Car"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car, 15));

                mMap.setOnMapClickListener(dest -> {

                    List<LatLng> route = generateRealisticRoute(car, dest);

                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(dest).title("Destination"));

                    mMap.addPolyline(new PolylineOptions()
                            .addAll(route)
                            .width(10f)
                            .color(0xFF2196F3));

                    startCarAnimation(route);

                    tripsRef.child(tripId).child("toLat").setValue(dest.latitude);
                    tripsRef.child(tripId).child("toLng").setValue(dest.longitude);
                    tripsRef.child(tripId).child("toLocation").setValue("Destination");
                    tripsRef.child(tripId).child("status").setValue(Trip.STATUS_IN_PROGRESS);
                    tripsRef.child(tripId).child("driverId").setValue(userId);

                    getSharedPreferences("session", MODE_PRIVATE)
                            .edit()
                            .putBoolean("pickingDestination", false)
                            .apply();

                    mMap.setOnMapClickListener(null);

                    Toast.makeText(MapActivity.this, "Ride started!", Toast.LENGTH_SHORT).show();

                    refreshUI();
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showRideMode(String tripId) {

        currentRideTripId = tripId;

        rideOverlay.setVisibility(View.VISIBLE);
        btnViewList.setVisibility(View.GONE);

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mMap == null) return;

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                LatLng from = new LatLng(trip.getFromLat(), trip.getFromLng());
                LatLng to = new LatLng(trip.getToLat(), trip.getToLng());

                List<LatLng> route = generateRealisticRoute(from, to);

                mMap.clear();

                mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10f)
                        .color(0xFF2196F3));

                startCarAnimation(route);

                txtRideInfo.setText("🚗 Ride in progress: " + trip.getCarName());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // =========================
    // 🚗 REALISTIC ROUTE
    // =========================
    private List<LatLng> generateRealisticRoute(LatLng start, LatLng end) {

        List<LatLng> base = new ArrayList<>();
        Random rand = new Random();

        int segments = 8;

        double latStep = (end.latitude - start.latitude) / segments;
        double lngStep = (end.longitude - start.longitude) / segments;

        LatLng current = start;
        base.add(current);

        for (int i = 1; i < segments; i++) {

            double lat = current.latitude + latStep;
            double lng = current.longitude + lngStep;

            if (i % 2 == 0) lat += (rand.nextDouble() - 0.5) * 0.003;
            else lng += (rand.nextDouble() - 0.5) * 0.003;

            current = new LatLng(lat, lng);
            base.add(current);
        }

        base.add(end);

        return smoothRoute(base);
    }

    private List<LatLng> smoothRoute(List<LatLng> input) {

        List<LatLng> smooth = new ArrayList<>();

        for (int i = 0; i < input.size() - 1; i++) {

            LatLng p0 = input.get(i);
            LatLng p1 = input.get(i + 1);

            for (int j = 0; j < 10; j++) {

                double t = j / 10.0;

                double lat = (1 - t) * p0.latitude + t * p1.latitude;
                double lng = (1 - t) * p0.longitude + t * p1.longitude;

                smooth.add(new LatLng(lat, lng));
            }
        }

        smooth.add(input.get(input.size() - 1));
        return smooth;
    }

    private void startCarAnimation(List<LatLng> route) {

        stopCarAnimation();

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(route.get(0))
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        carAnimator = ValueAnimator.ofFloat(0, route.size() - 1);
        carAnimator.setDuration(25000); // slower
        carAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        carAnimator.addUpdateListener(anim -> {

            float value = (float) anim.getAnimatedValue();
            int index = (int) value;

            if (index >= route.size() - 1) return;

            LatLng current = route.get(index);
            LatLng next = route.get(index + 1);

            carMarker.setPosition(current);

            float bearing = getBearing(current, next);
            carMarker.setRotation(bearing);

            mMap.animateCamera(CameraUpdateFactory.newLatLng(current));
        });

        carAnimator.start();
    }

    private void stopCarAnimation() {
        if (carAnimator != null) carAnimator.cancel();
    }

    private float getBearing(LatLng from, LatLng to) {

        double lat = Math.abs(from.latitude - to.latitude);
        double lng = Math.abs(from.longitude - to.longitude);

        if (from.latitude < to.latitude && from.longitude < to.longitude)
            return (float) Math.toDegrees(Math.atan(lng / lat));
        else if (from.latitude >= to.latitude && from.longitude < to.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (from.latitude >= to.latitude && from.longitude >= to.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}