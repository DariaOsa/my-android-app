package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.adapter.CarAdapter;
import edu.acg.carsharingapp.data.CarRepository;
import edu.acg.carsharingapp.model.Car;

public class CarListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private List<Car> carList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_list);

        // 🔙 Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Available Cars");
        }

        // 🔧 RecyclerView
        recyclerView = findViewById(R.id.recyclerCars);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔥 USE REPOSITORY (no more hardcoding)
        carList = CarRepository.getCars();

        // 🚗 Adapter → go directly to BookingActivity
        CarAdapter adapter = new CarAdapter(carList, car -> {

            Intent intent = new Intent(CarListActivity.this, BookingActivity.class);

            // 🔥 Pass full car object
            intent.putExtra("car", car);

            // Optional (if you want)
            intent.putExtra("userId", getSharedPreferences("session", MODE_PRIVATE)
                    .getString("userId", null));

            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    // 🔙 Back button
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}