package edu.acg.carsharingapp.adapter;

import android.content.Context;
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
import edu.acg.carsharingapp.model.Trip;
import edu.acg.carsharingapp.data.CarCatalog;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private List<Trip> tripList;
    private LatLng userLocation;
    private OnCarClickListener listener;

    public interface OnCarClickListener {
        void onItemClick(Trip trip);
    }

    public CarAdapter(List<Trip> trips, LatLng userLocation, OnCarClickListener listener) {
        this.tripList = trips;
        this.userLocation = userLocation;
        this.listener = listener;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {

        TextView name, meta, details, price, distance, status;
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

        Trip trip = tripList.get(position);
        Car car = CarCatalog.getCarByName(trip.getCarName());

        if (car == null) return;

        Context context = holder.itemView.getContext();

        // 🚗 NAME
        holder.name.setText(car.getDisplayName());

        // ⭐ CATEGORY + RATING
        if (holder.meta != null) {
            holder.meta.setText(context.getString(
                    R.string.car_meta,
                    car.getCategory(),
                    car.getRating()
            ));
        }

        // ⚙️ SPECS
        if (holder.details != null) {
            holder.details.setText(context.getString(
                    R.string.car_specs,
                    car.getSeatsText(context),
                    car.getFuelDisplay(context),
                    car.getTransmissionDisplay(context)
            ));
        }

        // 💶 PRICE
        holder.price.setText(trip.getFormattedPrice(context));

        // 🟢 STATUS (localized)
        if (holder.status != null) {
            holder.status.setText(getStatusText(context, trip.getStatus()));
        }

        // 🖼️ IMAGE
        if (holder.image != null) {
            holder.image.setImageResource(car.getImageResId());
        }

        // 📏 DISTANCE
        if (holder.distance != null && userLocation != null) {

            float[] results = new float[1];

            android.location.Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    trip.getCurrentLat(), trip.getCurrentLng(),
                    results
            );

            float km = results[0] / 1000f;

            holder.distance.setText(
                    context.getString(R.string.distance_km, km)
            );
        }

        // 🔥 CLICK
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(trip);
            }
        });
    }

    private String getStatusText(Context context, String status) {
        switch (status) {
            case Trip.STATUS_AVAILABLE:
                return context.getString(R.string.status_available);
            case Trip.STATUS_IN_PROGRESS:
                return context.getString(R.string.status_in_progress);
            case Trip.STATUS_COMPLETED:
                return context.getString(R.string.status_completed);
            default:
                return status;
        }
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }
}