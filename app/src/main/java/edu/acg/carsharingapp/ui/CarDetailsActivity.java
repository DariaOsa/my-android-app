package edu.acg.carsharingapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

        // 📦 Get Car object from Intent
        Car car = (Car) getIntent().getSerializableExtra("car");

        if (car != null) {

            // 🧾 Basic info
            tvName.setText(car.getName());
            tvPrice.setText(car.getPrice());
            tvDescription.setText(car.getDescription());

            // ⚙️ Features
            tvFeatures.setText(
                    "Seats: " + car.getSeats() +
                            "\nFuel: " + car.getFuelType() +
                            "\nTransmission: " + car.getTransmission()
            );

            // 🖼️ Image (only if available)
            if (car.getImageResId() != 0) {
                imgCar.setImageResource(car.getImageResId());
            }

        } else {
            tvName.setText("No car data");
        }

        // 🔙 Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 🔥 Book button action
        btnBook.setOnClickListener(v -> {
            Toast.makeText(this, "Booking feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}