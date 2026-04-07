package edu.acg.carsharingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private List<Car> carList;
    private OnItemClickListener listener;

    // 🔥 Click interface
    public interface OnItemClickListener {
        void onItemClick(Car car);
    }

    public CarAdapter(List<Car> carList, OnItemClickListener listener) {
        this.carList = carList;
        this.listener = listener;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {

        TextView name, price, details;
        ImageView image;

        public CarViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.tvCarName);
            price = itemView.findViewById(R.id.tvCarPrice);
            details = itemView.findViewById(R.id.tvCarDetails); // optional (add in XML)
            image = itemView.findViewById(R.id.imgCar); // optional (add in XML)
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

        // 🔥 NEW DATA USAGE
        holder.name.setText(car.getDisplayName());
        holder.price.setText(car.getFormattedPrice());

        // OPTIONAL polish (if you add TextView in XML)
        if (holder.details != null) {
            holder.details.setText(
                    car.getFuelType() + " • " + car.getTransmission()
            );
        }

        // OPTIONAL image (if you add ImageView in XML)
        if (holder.image != null) {
            holder.image.setImageResource(car.getImageResId());
        }

        // 🔥 Click
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