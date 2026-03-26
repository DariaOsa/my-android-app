package edu.acg.carsharingapp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import edu.acg.carsharingapp.R;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 📍 Add fake cars
        LatLng car1 = new LatLng(37.9838, 23.7275);
        LatLng car2 = new LatLng(37.98, 23.73);
        LatLng car3 = new LatLng(37.97, 23.72);

        mMap.addMarker(new MarkerOptions().position(car1).title("Car 1 - €20/day"));
        mMap.addMarker(new MarkerOptions().position(car2).title("Car 2 - €25/day"));
        mMap.addMarker(new MarkerOptions().position(car3).title("Car 3 - €30/day"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car1, 13));
    }
}