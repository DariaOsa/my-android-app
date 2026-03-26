package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.adapter.CarAdapter;
import edu.acg.carsharingapp.model.Car;

public class CarListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Car> carList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_list);

        // 🔙 Back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 🔧 Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerCars);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🚗 Localized car data
        carList = new ArrayList<>();

        carList.add(new Car(
                getString(R.string.car1_name),
                getString(R.string.car1_price),
                getString(R.string.car1_desc),
                R.drawable.car1,
                5,
                getString(R.string.fuel_petrol),
                getString(R.string.transmission_auto)
        ));

        carList.add(new Car(
                getString(R.string.car2_name),
                getString(R.string.car2_price),
                getString(R.string.car2_desc),
                R.drawable.car2,
                5,
                getString(R.string.fuel_diesel),
                getString(R.string.transmission_manual)
        ));

        carList.add(new Car(
                getString(R.string.car3_name),
                getString(R.string.car3_price),
                getString(R.string.car3_desc),
                R.drawable.car3,
                5,
                getString(R.string.fuel_petrol),
                getString(R.string.transmission_auto)
        ));

        // 🔥 Adapter with click → Details
        CarAdapter adapter = new CarAdapter(carList, car -> {
            Intent intent = new Intent(CarListActivity.this, CarDetailsActivity.class);
            intent.putExtra("car", car);
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