package edu.acg.carsharingapp.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;

import androidx.annotation.NonNull;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.model.Car;
import edu.acg.carsharingapp.data.CarCatalog;
import android.view.animation.LinearInterpolator;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String role;
    private String userId;

    private DatabaseReference tripsRef;
    private ValueEventListener tripsListener;

    private LinearLayout rideOverlay, searchCard;
    private TextView txtRideInfo, txtCurrentLocation;
    private EditText edtDestination;
    private Button btnEndRide, btnViewList, btnConfirmDestination;

    private ImageButton btnProfile;

    private String currentRideTripId = null;

    private Marker carMarker, destinationMarker;
    private ValueAnimator carAnimator;

    private LatLng userLocation, selectedDestination;

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
        txtCurrentLocation = findViewById(R.id.txtCurrentLocation);
        edtDestination = findViewById(R.id.edtDestination);
        searchCard = findViewById(R.id.searchCard);
        btnConfirmDestination = findViewById(R.id.btnConfirmDestination);
        btnEndRide = findViewById(R.id.btnEndRide);
        btnViewList = findViewById(R.id.btnViewList);
        btnProfile = findViewById(R.id.btnProfile);
        btnProfile.bringToFront();

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(MapActivity.this, ProfileActivity.class));
        });

        searchCard.setVisibility(View.GONE);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY");
        }

        edtDestination.setOnClickListener(v -> openAutocomplete());

        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(this, CarListActivity.class)));

        btnEndRide.setOnClickListener(v -> endRide());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) mapFragment.getMapAsync(this);

        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    seedTripsOnce();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // =========================
    // 🌱 SEED
    // =========================
    private void seedTripsOnce() {

        for (Car car : CarCatalog.getCars()) {

            String id = tripsRef.push().getKey();

            double lat = 37.9838 + (Math.random() - 0.5) * 0.05;
            double lng = 23.7275 + (Math.random() - 0.5) * 0.05;

            Trip t = new Trip(id, lat, lng, car.getSeats());
            t.setCarName(car.getDisplayName());
            t.setPrice(car.getPricePerTrip());

            tripsRef.child(id).setValue(t);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        enableMyLocation();
        fetchUserLocation();

        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        if (mMap == null) return;

        if (tripsListener != null) tripsRef.removeEventListener(tripsListener);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);

        String activeTrip = prefs.getString("activeTripId", null);
        boolean pickingDestination = prefs.getBoolean("pickingDestination", false);

        if (activeTrip != null && pickingDestination) {
            searchCard.setVisibility(View.VISIBLE);
            enableDestinationPicking(activeTrip);

        } else if (activeTrip != null) {
            searchCard.setVisibility(View.GONE);
            showRideMode(activeTrip);

        } else {
            searchCard.setVisibility(View.GONE);
            showBrowsingMode();
        }
    }

    private void showBrowsingMode() {

        rideOverlay.setVisibility(View.GONE);
        btnViewList.setVisibility(View.VISIBLE);

        tripsListener = tripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                mMap.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show;

                    if ("DRIVER".equals(role)) {
                        show = trip.isAvailable();
                    } else {
                        show = trip.isInProgress()
                                && trip.getDriverId() != null
                                && !trip.getDriverId().equals(userId)
                                && trip.getAvailableSeats() > 0;
                    }

                    if (!show) continue;

                    LatLng pos = new LatLng(
                            trip.getCurrentLat(),
                            trip.getCurrentLng()
                    );

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(trip.getCarName()));

                    if (m != null) m.setTag(trip.getTripId());
                }
                mMap.setOnMarkerClickListener(marker -> {

                    LatLng carPosition = marker.getPosition();
                    String tripId = (String) marker.getTag();

                    Intent i = new Intent(MapActivity.this, BookingActivity.class);
                    i.putExtra("tripId", tripId);

                    // ✅ PASS CAR POSITION
                    // 🚗 car location
                    i.putExtra("carLat", carPosition.latitude);
                    i.putExtra("carLng", carPosition.longitude);

// 📱 user location (real distance reference)
                    if (userLocation != null) {
                        i.putExtra("userLat", userLocation.latitude);
                        i.putExtra("userLng", userLocation.longitude);
                    }

                    startActivity(i);

                    return true;
                });

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasMarkers = false;

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show;

                    if ("DRIVER".equals(role)) {
                        show = trip.isAvailable();
                    } else {
                        show = trip.isInProgress()
                                && trip.getDriverId() != null
                                && !trip.getDriverId().equals(userId)
                                && trip.getAvailableSeats() > 0;
                    }

                    if (!show) continue;

                    LatLng pos = new LatLng(
                            trip.getCurrentLat(),
                            trip.getCurrentLng()
                    );

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(trip.getCarName()));

                    if (m != null) m.setTag(trip.getTripId());

                    // ✅ include in bounds
                    builder.include(pos);
                    hasMarkers = true;
                }

// ✅ AFTER loop → zoom to ALL cars
                if (hasMarkers) {
                    LatLngBounds bounds = builder.build();

                    mMap.setOnMapLoadedCallback(() ->
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)));
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // =========================
    // 🎯 DESTINATION PICK (FIXED)
    // =========================
    private void enableDestinationPicking(String tripId) {
        if (mMap == null) return;
        mMap.clear();

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                LatLng car = new LatLng(trip.getCurrentLat(), trip.getCurrentLng());
                // ✅ Show car location instead of user GPS
                new Thread(() -> {
                    String addr = getAddressFromLatLng(car);
                    runOnUiThread(() -> txtCurrentLocation.setText(addr));
                }).start();

                mMap.addMarker(new MarkerOptions().position(car).title("Your Car"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car, 15));

                btnConfirmDestination.setOnClickListener(v -> {

                    if (selectedDestination == null) {
                        Toast.makeText(MapActivity.this,
                                "Select destination first",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnConfirmDestination.setEnabled(false);

                    tripsRef.child(tripId).child("toLat")
                            .setValue(selectedDestination.latitude);

                    tripsRef.child(tripId).child("toLng")
                            .setValue(selectedDestination.longitude);

                    tripsRef.child(tripId).child("status")
                            .setValue(Trip.STATUS_IN_PROGRESS);

                    tripsRef.child(tripId).child("driverId")
                            .setValue(userId);

                    getSharedPreferences("session", MODE_PRIVATE)
                            .edit()
                            .putBoolean("pickingDestination", false)
                            .apply();

                    Toast.makeText(MapActivity.this,
                            "Ride started!",
                            Toast.LENGTH_SHORT).show();

                    refreshUI();
                });

                mMap.setOnMapClickListener(dest -> {

                    selectedDestination = dest;

                    new Thread(() -> {

                        List<LatLng> route = getRouteFromApi(car, dest);

                        runOnUiThread(() -> {

                            mMap.clear();

                            // 🚗 Car marker ALWAYS shows
                            mMap.addMarker(new MarkerOptions()
                                    .position(car)
                                    .title("Your Car"));

                            // 📍 Destination marker ALWAYS shows
                            destinationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(dest)
                                    .draggable(true)
                                    .title("Destination"));

                            // 📝 Address ALWAYS fills
                            edtDestination.setText(getAddressFromLatLng(dest));

                            // 🧭 Draw route ONLY if available
                            if (route != null && !route.isEmpty()) {
                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(route)
                                        .width(10f)
                                        .color(0xFF2196F3));
                            } else {
                                Toast.makeText(MapActivity.this,
                                        "Route unavailable",
                                        Toast.LENGTH_SHORT).show();
                            }

                            LatLngBounds.Builder builder = new LatLngBounds.Builder();

// Always include key points
                            builder.include(car);
                            builder.include(dest);

// Include full route (CRITICAL)
                            if (route != null && !route.isEmpty()) {
                                for (LatLng p : route) {
                                    builder.include(p);
                                }
                            }

                            LatLngBounds bounds = builder.build();

// Wait for map layout before applying bounds
                            mMap.setOnMapLoadedCallback(() -> {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
                            });
                        });

                    }).start();
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    // =========================
// 🚗 RIDE MODE (RESTORED)
// =========================
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

                LatLng from = new LatLng(
                        trip.getCurrentLat(),
                        trip.getCurrentLng()
                );

                // ✅ Update location label to car position
                new Thread(() -> {
                    String addr = getAddressFromLatLng(from);
                    runOnUiThread(() -> txtCurrentLocation.setText(addr));
                }).start();

                LatLng to = new LatLng(
                        trip.getToLat(),
                        trip.getToLng()
                );
                selectedDestination = to;

                new Thread(() -> {
                    List<LatLng> route = getRouteFromApi(from, to);

                    runOnUiThread(() -> {

                        mMap.clear();

                        mMap.addPolyline(new PolylineOptions()
                                .addAll(route)
                                .width(10f)
                                .color(0xFF2196F3));

                        if (route != null && !route.isEmpty()) {
                            startCarAnimation(route);
                        } else {
                            Toast.makeText(MapActivity.this,
                                    "Route unavailable",
                                    Toast.LENGTH_SHORT).show();
                        }

                        txtRideInfo.setText("🚗 Ride in progress: " + trip.getCarName());
                    });
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // =========================
    // 🛑 END RIDE
    // =========================
    private void endRide() {

        if (currentRideTripId == null) return;

        tripsRef.child(currentRideTripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip == null) return;

                        Map<String, Object> updates = new HashMap<>();

// ✅ ONLY update position when ride ENDS
                        if (selectedDestination != null) {
                            updates.put("currentLat", selectedDestination.latitude);
                            updates.put("currentLng", selectedDestination.longitude);
                        }

                        updates.put("status", Trip.STATUS_AVAILABLE);
                        updates.put("driverId", null);
                        updates.put("toLat", 0);
                        updates.put("toLng", 0);
                        updates.put("passengers", new HashMap<>());

                        tripsRef.child(currentRideTripId).updateChildren(updates)
                                .addOnCompleteListener(task -> {
                                    DatabaseReference historyRef = FirebaseDatabase.getInstance()
                                            .getReference("history")
                                            .child(userId);

                                    tripsRef.child(currentRideTripId)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                    Trip trip = snapshot.getValue(Trip.class);
                                                    if (trip == null) return;

                                                    String historyId = historyRef.push().getKey();
                                                    historyRef.child(historyId).setValue(trip);
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {}
                                            });

                                    // ✅ clear session AFTER DB update
                                    getSharedPreferences("session", MODE_PRIVATE)
                                            .edit()
                                            .remove("activeTripId")
                                            .remove("pickingDestination")
                                            .apply();

                                    currentRideTripId = null;

                                    stopCarAnimation();

                                    Toast.makeText(MapActivity.this,
                                            "Ride ended",
                                            Toast.LENGTH_SHORT).show();

                                    refreshUI(); // now safe ✔
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // =========================
    // 📍 LOCATION
    // =========================
    private void enableMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    private void fetchUserLocation() {

        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(this);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;

        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                updateCurrentLocationUI();
            }
        });
    }

    private void updateCurrentLocationUI() {

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String activeTrip = prefs.getString("activeTripId", null);
        boolean pickingDestination = prefs.getBoolean("pickingDestination", false);

        // ❌ DO NOT override when using car
        if (activeTrip != null) return;

        new Thread(() -> {
            String addr = getAddressFromLatLng(userLocation);
            runOnUiThread(() -> txtCurrentLocation.setText(addr));
        }).start();
    }

    private String getAddressFromLatLng(LatLng latLng) {
        try {
            android.location.Geocoder g =
                    new android.location.Geocoder(this, Locale.getDefault());
            List<android.location.Address> a =
                    g.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (a != null && !a.isEmpty()) return a.get(0).getAddressLine(0);
        } catch (Exception ignored) {}
        return "Selected location";
    }

    private void openAutocomplete() {

        List<Place.Field> fields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.LAT_LNG
        );

        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                fields).build(this);

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {

            Place place = Autocomplete.getPlaceFromIntent(data);

            LatLng dest = place.getLatLng();
            edtDestination.setText(place.getName());

            showPreviewRoute(dest);
        }
    }

    private void showPreviewRoute(LatLng destination) {

        if (userLocation == null) return;

        new Thread(() -> {
            List<LatLng> route = getRouteFromApi(userLocation, destination);

            runOnUiThread(() -> {

                mMap.clear();

                mMap.addMarker(new MarkerOptions().position(userLocation));
                mMap.addMarker(new MarkerOptions().position(destination));

                mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10f)
                        .color(0xFF2196F3));
            });
        }).start();
    }

    private List<LatLng> getRouteFromApi(LatLng origin, LatLng destination) {

        List<LatLng> path = new ArrayList<>();

        try {
            String urlStr = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin.latitude + "," + origin.longitude
                    + "&destination=" + destination.latitude + "," + destination.longitude
                    + "&key=AIzaSyA5JZ3w_M9F62uOy02zE4VM_GkdnItO1es";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";

            JSONObject json = new JSONObject(response);
            JSONArray routes = json.getJSONArray("routes");

            if (routes.length() == 0) return path;

            String encoded = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points");

            return decodePolyline(encoded);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    private List<LatLng> decodePolyline(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, lat = 0, lng = 0;

        while (index < encoded.length()) {

            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return poly;
    }

    private void startCarAnimation(List<LatLng> route) {
        if (route == null || route.size() < 2) return;

        stopCarAnimation();

        Bitmap icon = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.car_icon),
                100, 100, false
        );

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(route.get(0))
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(icon)));

        carAnimator = ValueAnimator.ofFloat(0, route.size() - 1);
        carAnimator.setDuration(60000);
        carAnimator.setInterpolator(new LinearInterpolator());

        carAnimator.addUpdateListener(animation -> {

            float value = (float) animation.getAnimatedValue();

            int index = (int) Math.floor(value);
            int nextIndex = Math.min(index + 1, route.size() - 1);

            float fraction = value - index;

            LatLng start = route.get(index);
            LatLng end = route.get(nextIndex);

            double lat = (1 - fraction) * start.latitude + fraction * end.latitude;
            double lng = (1 - fraction) * start.longitude + fraction * end.longitude;

            LatLng newPos = new LatLng(lat, lng);
            carMarker.setPosition(newPos);

            float bearing = getBearing(start, end);
            float smooth = smoothRotation(carMarker.getRotation(), bearing);
            carMarker.setRotation(smooth);

            mMap.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        });

        carAnimator.start();
    }
    private float getBearing(LatLng start, LatLng end) {
        double lat = Math.abs(start.latitude - end.latitude);
        double lng = Math.abs(start.longitude - end.longitude);

        if (start.latitude < end.latitude && start.longitude < end.longitude)
            return (float) Math.toDegrees(Math.atan(lng / lat));
        else if (start.latitude >= end.latitude && start.longitude < end.longitude)
            return (float) (90 - Math.toDegrees(Math.atan(lng / lat)) + 90);
        else if (start.latitude >= end.latitude && start.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (start.latitude < end.latitude && start.longitude >= end.longitude)
            return (float) (90 - Math.toDegrees(Math.atan(lng / lat)) + 270);

        return 0;
    }

    private float smoothRotation(float start, float end) {
        float diff = end - start;
        if (Math.abs(diff) > 180) {
            if (diff > 0) start += 360;
            else end += 360;
        }
        return start + (end - start);
    }

    private void stopCarAnimation() {
        if (carAnimator != null) carAnimator.cancel();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}