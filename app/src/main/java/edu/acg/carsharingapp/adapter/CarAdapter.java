package edu.acg.carsharingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private LatLng userLocation;
    private List<Car> carList;
    private OnCarClickListener listener;

    // 🔥 Click interface
    public interface OnCarClickListener {
        void onItemClick(Car car);
    }

    public CarAdapter(List<Car> cars, LatLng userLocation, OnCarClickListener listener) {
        this.carList = cars;
        this.userLocation = userLocation;
        this.listener = listener;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView meta;
        TextView details;
        TextView price;
        TextView distance;
        TextView status;
        ImageView image;

        public CarViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.txtCarName);
            meta = itemView.findViewById(R.id.txtMeta);
            details = itemView.findViewById(R.id.txtSpecs);
            price = itemView.findViewById(R.id.txtPrice);
            distance = itemView.findViewById(R.id.txtDistance);
            status = itemView.findViewById(R.id.txtStatus);
            image = itemView.findViewById(R.id.imgCar);
        }
    }

    @Override
    public CarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CarViewHolder holder, int position) {

        Car car = carList.get(position);

        // 🚗 NAME
        holder.name.setText(car.getDisplayName());

        // ⭐ CATEGORY + RATING
        if (holder.meta != null) {
            holder.meta.setText(car.getCategoryWithRating());
        }

        // ⚙️ SPECS
        if (holder.details != null) {
            holder.details.setText(
                    car.getSeatsText() + " • "
                            + car.getFuelDisplay() + " • "
                            + car.getShortTransmission()
            );
        }

        // 💶 PRICE
        holder.price.setText(car.getFormattedPrice());

        // 🟢 STATUS
        if (holder.status != null) {
            holder.status.setText("READY");
        }

        // 🖼️ IMAGE
        if (holder.image != null) {
            holder.image.setImageResource(car.getImageResId());
        }

        // ✅ FIXED DISTANCE (real, consistent)
        if (holder.distance != null && userLocation != null) {

            double carLat = car.getLatitude();
            double carLng = car.getLongitude();

            float[] results = new float[1];

            android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    carLat, carLng,
                    results
            );

            float km = results[0] / 1000f;

            holder.distance.setText(String.format("📏 %.2f km away", km));
        }

        // 🔥 CLICK
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(car);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }
}