package edu.acg.carsharingapp.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Locale;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.data.CarRepository;
import edu.acg.carsharingapp.model.Car;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

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

    private android.widget.ImageButton btnProfile;

    private String currentRideTripId = null;

    private Marker carMarker;
    private ValueAnimator carAnimator;

    private LatLng userLocation;
    private TextView txtCurrentLocation;
    private android.widget.EditText edtDestination;
    private Marker destinationMarker;

    private LinearLayout searchCard;

    private LatLng selectedDestination;
    private Button btnConfirmDestination;


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
        btnProfile = findViewById(R.id.btnProfile);
        txtCurrentLocation = findViewById(R.id.txtCurrentLocation);
        edtDestination = findViewById(R.id.edtDestination);
        searchCard = findViewById(R.id.searchCard);
        searchCard.setVisibility(View.GONE);
        btnConfirmDestination = findViewById(R.id.btnConfirmDestination);

// 🔑 Initialize Places
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(
                    getApplicationContext(),
                    "AIzaSyA5JZ3w_M9F62uOy02zE4VM_GkdnItO1es"
            );
        }
        edtDestination.setOnClickListener(v -> {

            List<Place.Field> fields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG
            );

            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY,
                    fields
            ).build(this);

            startActivityForResult(intent, 100);
        });

        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(this, CarListActivity.class))
        );

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        btnEndRide.setOnClickListener(v -> {

            if (currentRideTripId == null) {
                Toast.makeText(this, "No active trip!", Toast.LENGTH_SHORT).show();
                return;
            }

            tripsRef.child(currentRideTripId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            Trip trip = snapshot.getValue(Trip.class);
                            if (trip == null) return;

                            // ✅ 1. SAVE TO HISTORY
                            DatabaseReference historyRef = FirebaseDatabase.getInstance()
                                    .getReference("history");

                            historyRef.child(userId)
                                    .child(currentRideTripId)
                                    .setValue(trip);

                            // ✅ 2. RESET TRIP (MAKE IT AVAILABLE AGAIN)
                            Car car = CarRepository.getCars().get(0);
                            Map<String, Object> updates = new HashMap<>();

                            updates.put("status", Trip.STATUS_AVAILABLE);
                            updates.put("driverId", null);
                            updates.put("availableSeats", car.getSeats());

// ✅ RESET RIDE STATE PROPERLY
                            updates.put("toLat", 0);
                            updates.put("toLng", 0);
                            updates.put("toLocation", "");

// ✅ RESET PASSENGERS CLEANLY (NOT null)
                            updates.put("passengers", new HashMap<>());

                            tripsRef.child(currentRideTripId).updateChildren(updates);

                            // ✅ 3. CLEAN SESSION
                            getSharedPreferences("session", MODE_PRIVATE)
                                    .edit()
                                    .remove("activeTripId")
                                    .remove("pickingDestination")
                                    .apply();

                            currentRideTripId = null;
                            stopCarAnimation();
                            refreshUI();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
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
            t.setStatus(Trip.STATUS_AVAILABLE);


            if (i < 3) {
                t.setStatus(Trip.STATUS_AVAILABLE);
                t.setDriverId(null); // no driver yet
            } else {
                t.setStatus(Trip.STATUS_IN_PROGRESS);
                t.setDriverId("testDriver"); // active ride
            }

            updates.put(id, t);
        }

        tripsRef.updateChildren(updates);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
        fetchUserLocation();
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
            btnProfile.setVisibility(View.GONE);
            searchCard.setVisibility(View.VISIBLE); // ✅ SHOW HERE
            enableDestinationPicking(activeTrip);

        } else if (activeTrip != null) {

            searchCard.setVisibility(View.GONE); // ❌ HIDE
            showRideMode(activeTrip);

        } else {

            searchCard.setVisibility(View.GONE); // ❌ HIDE
            showBrowsingMode();
        }
    }

    private void showBrowsingMode() {

        rideOverlay.setVisibility(View.GONE);
        currentRideTripId = null;
        btnViewList.setVisibility(View.VISIBLE);
        btnProfile.setVisibility(View.VISIBLE);

        tripsListener = tripsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mMap == null) return;

                mMap.clear();

                for (DataSnapshot snap : snapshot.getChildren()) {

                    Trip trip = snap.getValue(Trip.class);
                    if (trip == null) continue;

                    boolean show;

                    if ("DRIVER".equals(role)) {
                        // Driver sees only free cars
                        show = Trip.STATUS_AVAILABLE.equals(trip.getStatus());
                    } else {
                        // Passenger sees ONLY real active rides from other drivers
                        show = Trip.STATUS_IN_PROGRESS.equals(trip.getStatus())
                                && trip.getDriverId() != null
                                && !trip.getDriverId().equals(userId)
                                && trip.hasAvailableSeats();
                    }

                    if (!show) continue;

                    LatLng pos = new LatLng(trip.getFromLat(), trip.getFromLng());

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(trip.getCarName()));

                    if (m != null) m.setTag(trip.getTripId());
                }

                if (userLocation != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(37.9838, 23.7275), 12));
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mMap.setOnMarkerClickListener(marker -> {

            if (userLocation == null) {
                Toast.makeText(this, "Getting your location... try again", Toast.LENGTH_SHORT).show();
                return true;
            }

            Intent i = new Intent(this, BookingActivity.class);
            i.putExtra("tripId", (String) marker.getTag());

            // ✅ PASS USER LOCATION (same as list)
            i.putExtra("userLat", userLocation.latitude);
            i.putExtra("userLng", userLocation.longitude);

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

// 👇 ADD THIS BLOCK HERE
                btnConfirmDestination.setOnClickListener(v -> {

                    if (selectedDestination == null) {
                        Toast.makeText(MapActivity.this, "Select destination first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLng dest = selectedDestination;

                    new Thread(() -> {
                        List<LatLng> route = getRouteFromApi(car, dest);

                        runOnUiThread(() -> {

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
                    }).start();
                });

                mMap.addMarker(new MarkerOptions().position(car).title("Your Car"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car, 15));

                mMap.setOnMapClickListener(dest -> {

                    selectedDestination = dest;

                    new Thread(() -> {
                        List<LatLng> route = getRouteFromApi(car, dest);

                        runOnUiThread(() -> {

                            // ✅ Remove old marker
                            if (destinationMarker != null) {
                                destinationMarker.remove();
                            }

                            // ✅ Add draggable destination pin
                            destinationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(dest)
                                    .draggable(true)
                                    .title("Destination"));

                            // ✅ Fill destination input
                            edtDestination.setText(getAddressFromLatLng(dest));

                            // ✅ Draw route
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(route)
                                    .width(10f)
                                    .color(0xFF2196F3));
                        });
                    }).start();
                });
                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

                    @Override public void onMarkerDragStart(Marker marker) {}

                    @Override public void onMarkerDrag(Marker marker) {}

                    @Override
                    public void onMarkerDragEnd(Marker marker) {

                        LatLng newPos = marker.getPosition();
                        selectedDestination = newPos;

                        edtDestination.setText(getAddressFromLatLng(newPos));

                        new Thread(() -> {
                            List<LatLng> route = getRouteFromApi(car, newPos);

                            runOnUiThread(() -> {

                                mMap.addPolyline(new PolylineOptions()
                                        .addAll(route)
                                        .width(10f)
                                        .color(0xFF2196F3));
                            });
                        }).start();
                    }
                });

            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showRideMode(String tripId) {

        currentRideTripId = tripId;

        rideOverlay.setVisibility(View.VISIBLE);
        btnViewList.setVisibility(View.GONE);
        btnProfile.setVisibility(View.GONE);

        tripsRef.child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (mMap == null) return;

                Trip trip = snapshot.getValue(Trip.class);
                if (trip == null) return;

                LatLng from = new LatLng(trip.getFromLat(), trip.getFromLng());
                LatLng to = new LatLng(trip.getToLat(), trip.getToLng());

                new Thread(() -> {
                    List<LatLng> route = getRouteFromApi(from, to);

                    runOnUiThread(() -> {

                        mMap.clear();

                        mMap.addPolyline(new PolylineOptions()
                                .addAll(route)
                                .width(10f)
                                .color(0xFF2196F3));

                        startCarAnimation(route);

                        txtRideInfo.setText("🚗 Ride in progress: " + trip.getCarName());
                    });
                }).start();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==========================
    // REAL ROUTE (Directions API)
    // ==========================

    private List<LatLng> getRouteFromApi(LatLng origin, LatLng destination) {

        List<LatLng> path = new ArrayList<>();

        try {
            String apiKey = "AIzaSyA5JZ3w_M9F62uOy02zE4VM_GkdnItO1es";

            String urlStr = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "origin=" + origin.latitude + "," + origin.longitude
                    + "&destination=" + destination.latitude + "," + destination.longitude
                    + "&mode=driving"
                    + "&key=" + apiKey;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            JSONObject json = new JSONObject(response);
            JSONArray routes = json.getJSONArray("routes");

            if (routes.length() == 0) return path;

            JSONObject route = routes.getJSONObject(0);
            String encoded = route.getJSONObject("overview_polyline").getString("points");

            path = decodePolyline(encoded);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    private List<LatLng> decodePolyline(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {

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

        stopCarAnimation();

        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.car_icon);
        Bitmap scaled = Bitmap.createScaledBitmap(original, 100, 100, false);

        carMarker = mMap.addMarker(new MarkerOptions()
                .position(route.get(0))
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(scaled)));

        carAnimator = ValueAnimator.ofFloat(0, route.size() - 1);
        carAnimator.setDuration(60000);
        carAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        carAnimator.addUpdateListener(anim -> {

            float value = (float) anim.getAnimatedValue();
            int index = (int) value;

            if (index >= route.size() - 1) return;

            LatLng current = route.get(index);
            LatLng next = route.get(index + 1);

            carMarker.setPosition(current);
            carMarker.setRotation(getBearing(current, next));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
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

    // ==========================
// LOCATION METHODS
// ==========================

    private void enableMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);

        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
            );
        }
    }

    private void fetchUserLocation() {

        com.google.android.gms.location.FusedLocationProviderClient client =
                com.google.android.gms.location.LocationServices
                        .getFusedLocationProviderClient(this);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;

        client.getLastLocation().addOnSuccessListener(location -> {

            if (location != null) {
                userLocation = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );
                updateCurrentLocationUI();

            } else {
                // 🔥 THIS PART FIXES YOUR ISSUE
                client.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                ).addOnSuccessListener(loc -> {
                    if (loc != null) {
                        userLocation = new LatLng(
                                loc.getLatitude(),
                                loc.getLongitude()
                        );
                        updateCurrentLocationUI();
                    }
                });
            }
        });
    }
    private void showPreviewRoute(LatLng destination) {

        if (userLocation == null) {
            Toast.makeText(this, "Waiting for your location...", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            List<LatLng> route = getRouteFromApi(userLocation, destination);

            runOnUiThread(() -> {

                mMap.clear();

                mMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title("You"));

                mMap.addMarker(new MarkerOptions()
                        .position(destination)
                        .title("Destination"));

                mMap.addPolyline(new PolylineOptions()
                        .addAll(route)
                        .width(10f)
                        .color(0xFF2196F3));
            });
        }).start();
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
    private String getAddressFromLatLng(LatLng latLng) {
        try {
            android.location.Geocoder geocoder =
                    new android.location.Geocoder(this, Locale.getDefault());

            List<android.location.Address> addresses =
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Selected location";
    }

    private void updateCurrentLocationUI() {

        if (txtCurrentLocation == null || userLocation == null) return;

        new Thread(() -> {
            String address = getAddressFromLatLng(userLocation);

            runOnUiThread(() -> {
                txtCurrentLocation.setText(address);
            });
        }).start();
    }
    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
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
}