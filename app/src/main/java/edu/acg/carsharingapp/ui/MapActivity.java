package edu.acg.carsharingapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import edu.acg.carsharingapp.R;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.loadLocale(newBase));
    }

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

        ImageView imgEnglish = findViewById(R.id.imgEnglish);
        ImageView imgGreek = findViewById(R.id.imgGreek);

        imgEnglish.setOnClickListener(v -> setLanguage("en"));
        imgGreek.setOnClickListener(v -> setLanguage("el"));

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
                .snippet(getString(R.string.car1_price)));
        car1Marker.setTag(R.drawable.car1);

        Marker car2Marker = mMap.addMarker(new MarkerOptions()
                .position(car2)
                .title(getString(R.string.car2_name))
                .snippet(getString(R.string.car2_price)));
        car2Marker.setTag(R.drawable.car2);

        Marker car3Marker = mMap.addMarker(new MarkerOptions()
                .position(car3)
                .title(getString(R.string.car3_name))
                .snippet(getString(R.string.car3_price)));
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

    private void setLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);

        // Restart activity properly
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}