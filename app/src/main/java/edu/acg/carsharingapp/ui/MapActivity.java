package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import edu.acg.carsharingapp.R;

public class MapActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnViewList = findViewById(R.id.btnViewList);
        btnViewList.setOnClickListener(v ->
                startActivity(new Intent(MapActivity.this, CarListActivity.class))
        );

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

        LatLng car1 = new LatLng(37.9838, 23.7275);
        LatLng car2 = new LatLng(37.98, 23.73);
        LatLng car3 = new LatLng(37.97, 23.72);

        Marker car1Marker = mMap.addMarker(new MarkerOptions()
                .position(car1)
                .title(getString(R.string.car1_name))
                .snippet(getString(R.string.price_per_day, getString(R.string.car1_price_value))));
        car1Marker.setTag(R.drawable.car1);

        Marker car2Marker = mMap.addMarker(new MarkerOptions()
                .position(car2)
                .title(getString(R.string.car2_name))
                .snippet(getString(R.string.price_per_day, getString(R.string.car2_price_value))));
        car2Marker.setTag(R.drawable.car2);

        Marker car3Marker = mMap.addMarker(new MarkerOptions()
                .position(car3)
                .title(getString(R.string.car3_name))
                .snippet(getString(R.string.price_per_day, getString(R.string.car3_price_value))));
        car3Marker.setTag(R.drawable.car3);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(car1, 13));

        mMap.setOnMarkerClickListener(marker -> {
            Intent intent = new Intent(MapActivity.this, CarDetailsActivity.class);
            intent.putExtra("carName", marker.getTitle());
            intent.putExtra("price", marker.getSnippet());

            if (marker.getTag() != null) {
                intent.putExtra("imageResId", (int) marker.getTag());
            }

            startActivity(intent);
            return false;
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}