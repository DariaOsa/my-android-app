package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);

        // 🔍 Find views
        ImageView imgCar = findViewById(R.id.imgCar);
        TextView tvName = findViewById(R.id.tvCarName);
        TextView tvPrice = findViewById(R.id.tvCarPrice);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvFeatures = findViewById(R.id.tvFeatureDetails);
        Button btnBook = findViewById(R.id.btnBook);

        // 🔙 Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 📦 TRY to get full Car object (from LIST)
        Car car = (Car) getIntent().getSerializableExtra("car");

        if (car != null) {
            // ✅ FROM LIST
            tvName.setText(car.getName());
            tvPrice.setText(car.getPrice());
            tvDescription.setText(car.getDescription());

            String features = getString(R.string.seats) + ": " + car.getSeats() + "\n" +
                    getString(R.string.fuel) + ": " + car.getFuelType() + "\n" +
                    getString(R.string.transmission) + ": " + car.getTransmission();

            tvFeatures.setText(features);

            if (car.getImageResId() != 0) {
                imgCar.setImageResource(car.getImageResId());
            }

        } else {
            // ✅ FROM MAP
            String carName = getIntent().getStringExtra("carName");
            String price = getIntent().getStringExtra("price");
            int imageResId = getIntent().getIntExtra("imageResId", 0);

            if (carName != null) {
                tvName.setText(carName);
            } else {
                tvName.setText(getString(R.string.unknown_car));
            }

            if (price != null) {
                tvPrice.setText(price);
            } else {
                tvPrice.setText(getString(R.string.default_price));
            }

            tvDescription.setText(getString(R.string.default_description));

            tvFeatures.setText(getString(R.string.features_details));

            if (imageResId != 0) {
                imgCar.setImageResource(imageResId);
            } else {
                imgCar.setImageResource(R.drawable.car1); // fallback
            }
        }

        // 🔥 Book button
        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(CarDetailsActivity.this, BookingActivity.class);

            intent.putExtra("carName", tvName.getText().toString());
            intent.putExtra("price", tvPrice.getText().toString());

            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}