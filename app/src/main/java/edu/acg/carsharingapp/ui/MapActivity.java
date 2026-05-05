package edu.acg.carsharingapp.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.util.Log;

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
    private Button btnEndRide, btnConfirmDestination;
    private ImageButton btnViewList;

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

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(),
                    getString(R.string.google_maps_key));
        }

        // 🔍 Autocomplete on click (optional UX)
        edtDestination.setOnClickListener(v -> openAutocomplete());

// ⌨️ Manual typing support
        edtDestination.setOnEditorActionListener((v, actionId, event) -> {

            String text = edtDestination.getText().toString().trim();

            if (!text.isEmpty()) {
                handleTypedAddress(text);
            }

            return true;
        });

        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(this, CarListActivity.class)));

        btnEndRide.setOnClickListener(v -> endRide());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) mapFragment.getMapAsync(this);

        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) seedTripsOnce();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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

        enableMyLocation();       // 🔴 first
        fetchUserLocation();

        // ✅ THEN disable Google UI buttons
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        refreshUI();
    }

    private void refreshUI() {

        if (mMap == null) return;

        if (tripsListener != null) tripsRef.removeEventListener(tripsListener);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);

        String activeTrip = prefs.getString("activeTripId", null);
        boolean pickingDestination = prefs.getBoolean("pickingDestination", false);
        Log.d("MAP_DEBUG", "activeTrip=" + activeTrip + " picking=" + pickingDestination);

        if (activeTrip != null) {
            btnProfile.setVisibility(View.GONE);
        } else {
            btnProfile.setVisibility(View.VISIBLE);
        }

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

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasMarkers = false;

// 🔵 include user location
                if (userLocation != null) {
                    builder.include(userLocation);
                }

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    // 🔥 ROLE FILTERING (THIS is what you were missing)
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

                    // ✅ include in camera bounds
                    builder.include(pos);
                    hasMarkers = true;
                }
                if (hasMarkers) {
                    LatLngBounds bounds = builder.build();

                    mMap.setOnMapLoadedCallback(() ->
                            mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 150)
                            )
                    );
                } else if (userLocation != null) {
                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(userLocation, 14)
                    );
                }


                mMap.setOnMarkerClickListener(marker -> {

                    LatLng carPosition = marker.getPosition();
                    String tripId = (String) marker.getTag();

                    Intent i = new Intent(MapActivity.this, BookingActivity.class);
                    i.putExtra("tripId", tripId);
                    i.putExtra("carLat", carPosition.latitude);
                    i.putExtra("carLng", carPosition.longitude);

                    if (userLocation != null) {
                        i.putExtra("userLat", userLocation.latitude);
                        i.putExtra("userLng", userLocation.longitude);
                    }

                    startActivity(i);
                    return true;
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void enableDestinationPicking(String tripId) {
        if (mMap == null) return;

        mMap.clear();

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                LatLng car = new LatLng(trip.getCurrentLat(), trip.getCurrentLng());

                // 🔹 show address
                new Thread(() -> {
                    String addr = getAddressFromLatLng(car);
                    runOnUiThread(() -> txtCurrentLocation.setText(addr));
                }).start();

                // 🔹 car marker
                mMap.addMarker(new MarkerOptions()
                        .position(car)
                        .title(getString(R.string.your_car)));

                // 🔥 THIS WAS MISSING → zoom
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car, 15));

                btnConfirmDestination.setOnClickListener(v -> {

                    if (selectedDestination == null) {
                        selectedDestination = tryGetDestinationFromText();

                        if (selectedDestination == null) {
                            Toast.makeText(MapActivity.this,
                                    getString(R.string.select_destination_first),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
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
                            getString(R.string.ride_started),
                            Toast.LENGTH_SHORT).show();

                    refreshUI();
                });

                mMap.setOnMapClickListener(dest -> {

                    selectedDestination = dest;

                    new Thread(() -> {

                        List<LatLng> route = getRouteFromApi(car, dest);

                        runOnUiThread(() -> {

                            mMap.clear();

                            mMap.addMarker(new MarkerOptions()
                                    .position(car)
                                    .title(getString(R.string.your_car)));

                            if (destinationMarker != null) {
                                destinationMarker.remove();
                            }

                            destinationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(dest)
                                    .title(getString(R.string.destination)));

                            edtDestination.setText(getAddressFromLatLng(dest));
                            askOpenInMaps(car, dest);

                            if (route != null && !route.isEmpty()) {
                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(route)
                                        .width(10f)
                                        .color(0xFF2196F3));
                            } else {
                                Toast.makeText(MapActivity.this,
                                        getString(R.string.route_unavailable),
                                        Toast.LENGTH_SHORT).show();
                            }

                            double distanceKm = calculateRouteDistanceKm(route);
                            double price = trip.getPrice() * distanceKm;

                            Toast.makeText(MapActivity.this,
                                    getString(R.string.estimated_price, price),
                                    Toast.LENGTH_LONG).show();

                            // 🔥 zoom to full route
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(car);
                            builder.include(dest);

                            if (route != null) {
                                for (LatLng p : route) builder.include(p);
                            }

                            LatLngBounds bounds = builder.build();

                            mMap.setOnMapLoadedCallback(() ->
                                    mMap.animateCamera(
                                            CameraUpdateFactory.newLatLngBounds(bounds, 200)));
                        });

                    }).start();
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

                LatLng from = new LatLng(
                        trip.getCurrentLat(),
                        trip.getCurrentLng()
                );

                // ✅ update location text
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

                        if (route != null && !route.isEmpty()) {

                            // ✅ draw route
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(route)
                                    .width(10f)
                                    .color(0xFF2196F3));

                            // 🔥 THIS IS CRITICAL → animation
                            startCarAnimation(route);

                        } else {
                            Toast.makeText(MapActivity.this,
                                    getString(R.string.route_unavailable),
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ✅ ride text
                        txtRideInfo.setText(
                                getString(R.string.ride_in_progress) + ": " + trip.getCarName()
                        );

                        // ✅ zoom to route
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(from);
                        builder.include(to);

                        if (route != null) {
                            for (LatLng p : route) builder.include(p);
                        }

                        LatLngBounds bounds = builder.build();

                        mMap.setOnMapLoadedCallback(() ->
                                mMap.animateCamera(
                                        CameraUpdateFactory.newLatLngBounds(bounds, 200)));
                    });

                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void endRide() {

        if (currentRideTripId == null) return;

        tripsRef.child(currentRideTripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip == null) return;

                        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                                .getReference("history")
                                .child(userId);

                        new Thread(() -> {

                            LatLng from = new LatLng(trip.getCurrentLat(), trip.getCurrentLng());
                            LatLng to = new LatLng(trip.getToLat(), trip.getToLng());

                            List<LatLng> route = getRouteFromApi(from, to);

                            double distanceKm = 0;

                            if (route != null && !route.isEmpty()) {
                                distanceKm = calculateRouteDistanceKm(route);
                            }

                            // fallback distance
                            if (distanceKm == 0) {
                                float[] results = new float[1];
                                android.location.Location.distanceBetween(
                                        from.latitude, from.longitude,
                                        to.latitude, to.longitude,
                                        results
                                );
                                distanceKm = results[0] / 1000.0;
                            }

                            double finalPrice = trip.getPrice() * distanceKm;
                            trip.setFinalPrice(finalPrice);

// ✅ FIX 1: mark as completed
                            trip.setStatus(Trip.STATUS_COMPLETED);
                            trip.setCompletedAt(System.currentTimeMillis());

// ✅ FIX 2: save real addresses
                            String fromAddress = getAddressFromLatLng(from);
                            String toAddress = getAddressFromLatLng(to);

                            trip.setFromAddress(fromAddress);
                            trip.setToAddress(toAddress);

                            runOnUiThread(() -> {

                                // ✅ save history
                                String historyId = historyRef.push().getKey();
                                historyRef.child(historyId).setValue(trip);

                                // ✅ reset trip
                                Map<String, Object> updates = new HashMap<>();

                                if (selectedDestination != null) {
                                    updates.put("currentLat", selectedDestination.latitude);
                                    updates.put("currentLng", selectedDestination.longitude);
                                }

                                updates.put("status", Trip.STATUS_AVAILABLE);
                                updates.put("driverId", null);
                                updates.put("toLat", 0);
                                updates.put("toLng", 0);
                                updates.put("passengers", new HashMap<>());

                                tripsRef.child(currentRideTripId).updateChildren(updates);

                                // ✅ CLEAR SESSION (CRITICAL)
                                getSharedPreferences("session", MODE_PRIVATE)
                                        .edit()
                                        .remove("activeTripId")
                                        .remove("pickingDestination")
                                        .apply();

                                currentRideTripId = null;

                                // ✅ STOP animation
                                stopCarAnimation();

                                Toast.makeText(MapActivity.this,
                                        getString(R.string.ride_ended),
                                        Toast.LENGTH_SHORT).show();

                                // 🔥 THIS WAS MISSING
                                refreshUI();
                            });

                        }).start();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void enableMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
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
            }
        });
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
    private void handleTypedAddress(String addressText) {

        new Thread(() -> {

            try {
                android.location.Geocoder geocoder =
                        new android.location.Geocoder(this, Locale.getDefault());

                List<android.location.Address> results =
                        geocoder.getFromLocationName(addressText, 1);

                if (results != null && !results.isEmpty()) {

                    LatLng dest = new LatLng(
                            results.get(0).getLatitude(),
                            results.get(0).getLongitude()
                    );

                    selectedDestination = dest;

                    runOnUiThread(() -> drawRouteManually(dest));

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    getString(R.string.location_not_found),
                                    Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start(); // ✅
    }

    private void drawRouteManually(LatLng dest) {

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String tripId = prefs.getString("activeTripId", null);

        if (tripId == null) return;

        tripsRef.child(tripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip == null) return;

                        LatLng car = new LatLng(trip.getCurrentLat(), trip.getCurrentLng());
                        selectedDestination = dest;

                        new Thread(() -> {

                            List<LatLng> route = getRouteFromApi(car, dest);

                            runOnUiThread(() -> {

                                mMap.clear();

// 🚗 Car marker
                                mMap.addMarker(new MarkerOptions()
                                        .position(car)
                                        .title(getString(R.string.your_car)));

// 📍 Destination marker
                                destinationMarker = mMap.addMarker(new MarkerOptions()
                                        .position(dest)
                                        .title(getString(R.string.destination)));

// 📝 Update text
                                edtDestination.setText(getAddressFromLatLng(dest));

// 🗺️ Draw route
                                if (route != null && !route.isEmpty()) {
                                    mMap.addPolyline(new PolylineOptions()
                                            .addAll(route)
                                            .width(10f)
                                            .color(0xFF2196F3));
                                }

// 💰 Show price
                                double distanceKm = calculateRouteDistanceKm(route);
                                double price = trip.getPrice() * distanceKm;

                                Toast.makeText(MapActivity.this,
                                        getString(R.string.estimated_price, price),
                                        Toast.LENGTH_LONG).show();

                                // 🧭 Zoom to route
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                builder.include(car);
                                builder.include(dest);

                                if (route != null) {
                                    for (LatLng p : route) builder.include(p);
                                }





                            });

                        }).start();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    // =========================
// 🌐 ROUTE API
// =========================
    private List<LatLng> getRouteFromApi(LatLng origin, LatLng destination) {

        List<LatLng> path = new ArrayList<>();

        try {
            String urlStr = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin.latitude + "," + origin.longitude
                    + "&destination=" + destination.latitude + "," + destination.longitude
                    + "&key=" + getString(R.string.google_maps_key);

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

    // =========================
// 🔓 POLYLINE DECODER
// =========================
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

    // =========================
// 📏 DISTANCE
// =========================
    private double calculateRouteDistanceKm(List<LatLng> route) {

        if (route == null || route.size() < 2) return 0;

        double distance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            float[] results = new float[1];

            android.location.Location.distanceBetween(
                    route.get(i).latitude, route.get(i).longitude,
                    route.get(i + 1).latitude, route.get(i + 1).longitude,
                    results
            );

            distance += results[0];
        }

        return distance / 1000.0;
    }
    private void openInGoogleMaps(LatLng from, LatLng to) {

        String uri = "https://www.google.com/maps/dir/?api=1"
                + "&origin=" + from.latitude + "," + from.longitude
                + "&destination=" + to.latitude + "," + to.longitude
                + "&travelmode=driving";

        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        startActivity(intent);
    }
    private void askOpenInMaps(LatLng from, LatLng to) {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.open_in_maps_title))
                .setMessage(getString(R.string.open_in_maps_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    openInGoogleMaps(from, to);
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    // =========================
// 📍 ADDRESS
// =========================
    private String getAddressFromLatLng(LatLng latLng) {

        try {
            android.location.Geocoder g =
                    new android.location.Geocoder(this, Locale.getDefault());

            List<android.location.Address> a =
                    g.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (a != null && !a.isEmpty()) {
                android.location.Address addr = a.get(0);
                return addr.getThoroughfare() + ", " + addr.getLocality();
            }

        } catch (Exception ignored) {}

        return getString(R.string.selected_location);
    }

    // =========================
// 🚗 CAR ANIMATION
// =========================
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

            // 🔥 camera follows the car
            CameraPosition position = new CameraPosition.Builder()
                    .target(newPos)
                    .zoom(17f)
                    .tilt(45f)   // 🔥 adds perspective
                    .build();

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        });

        carAnimator.start();
    }

    // =========================
// 📐 BEARING
// =========================
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

    // =========================
// 🔄 ROTATION SMOOTHING
// =========================
    private float smoothRotation(float start, float end) {

        float diff = end - start;

        if (Math.abs(diff) > 180) {
            if (diff > 0) start += 360;
            else end += 360;
        }

        return start + (end - start);
    }
    private LatLng tryGetDestinationFromText() {

        String text = edtDestination.getText().toString();

        try {
            android.location.Geocoder geocoder =
                    new android.location.Geocoder(this, Locale.getDefault());

            List<android.location.Address> results =
                    geocoder.getFromLocationName(text, 1);

            if (results != null && !results.isEmpty()) {
                return new LatLng(
                        results.get(0).getLatitude(),
                        results.get(0).getLongitude()
                );
            }

        } catch (Exception ignored) {}

        return null;
    }

    // =========================
// ⛔ STOP ANIMATION
// =========================
    private void stopCarAnimation() {
        if (carAnimator != null) carAnimator.cancel();
    }
}