package edu.acg.carsharingapp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.adapter.CarAdapter;
import edu.acg.carsharingapp.model.Car;
import android.content.Intent;

public class CarListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_list);

        // Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerCars);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create sample car data
        List<Car> carList = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {

            String name = "Car " + i;
            String price = "€" + (10 + i) + "/day";
            String description = "This is a comfortable and reliable car number " + i;

            int[] images = {
                    R.drawable.car1,
                    R.drawable.car2,
                    R.drawable.car3,
                    R.drawable.car4,
                    R.drawable.car5
            };

            int image = images[i % images.length];

            int seats = 5;
            String fuelType = "Petrol";
            String transmission = "Automatic";

            carList.add(new Car(
                    name,
                    price,
                    description,
                    image,
                    seats,
                    fuelType,
                    transmission
            ));
        }

        // Set adapter WITH click listener
        CarAdapter adapter = new CarAdapter(carList, car -> {
            Intent intent = new Intent(CarListActivity.this, CarDetailsActivity.class);
            intent.putExtra("car", car); // ✅ no cast needed
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}