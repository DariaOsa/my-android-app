package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarDetailsActivity extends BaseActivity {

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

        // 📦 Get Car object (if coming from list)
        Car car = (Car) getIntent().getSerializableExtra("car");

        if (car != null) {
            // ✅ FROM LIST
            tvName.setText(car.getName());

            // ✅ Proper price formatting
            tvPrice.setText(getString(R.string.price_per_day, car.getPrice()));

            tvDescription.setText(car.getDescription());

            // ✅ Proper features formatting
            String features = getString(
                    R.string.car_features,
                    car.getSeats(),
                    car.getFuelType(),
                    car.getTransmission()
            );
            tvFeatures.setText(features);

            if (car.getImageResId() != 0) {
                imgCar.setImageResource(car.getImageResId());
            }

        } else {
            // ✅ FROM MAP (fallback)
            String carName = getIntent().getStringExtra("carName");
            String price = getIntent().getStringExtra("price");
            int imageResId = getIntent().getIntExtra("imageResId", 0);

            // Name
            if (carName != null) {
                tvName.setText(carName);
            } else {
                tvName.setText(R.string.unknown_car);
            }

            // Price (formatted)
            if (price != null) {
                tvPrice.setText(getString(R.string.price_per_day, price));
            } else {
                tvPrice.setText(
                        getString(
                                R.string.price_per_day,
                                getString(R.string.default_price_value)
                        )
                );
            }

            // Description
            tvDescription.setText(R.string.default_description);

            // Features (default values)
            tvFeatures.setText(
                    getString(
                            R.string.car_features,
                            5,
                            getString(R.string.fuel_petrol),
                            getString(R.string.transmission_auto)
                    )
            );

            // Image
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

    // 🔙 Handle toolbar back button
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}