package edu.acg.carsharingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import edu.acg.carsharingapp.R;

public class BookingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // 🔙 Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 🔍 Find views
        TextView tvMessage = findViewById(R.id.tvMessage);
        Button btnDone = findViewById(R.id.btnDone);

        // 📦 Get data from intent
        String carName = getIntent().getStringExtra("carName");
        String price = getIntent().getStringExtra("price");

        // 📝 Set message (localized)
        String message = getString(
                R.string.booking_message,
                getString(R.string.booking_success),
                (carName != null ? carName : getString(R.string.unknown_car)),
                (price != null ? price : getString(R.string.price_per_day, getString(R.string.default_price_value)))
        );

        tvMessage.setText(message);

        // 🔥 DONE → go back to MAP
        btnDone.setText(R.string.back_to_map);

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(BookingActivity.this, MapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // 🔙 Handle toolbar back button
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}