package edu.acg.carsharingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private List<Car> carList;
    private OnItemClickListener listener;

    // 🔥 Interface for click handling
    public interface OnItemClickListener {
        void onItemClick(Car car);
    }

    // 🔥 Updated constructor
    public CarAdapter(List<Car> carList, OnItemClickListener listener) {
        this.carList = carList;
        this.listener = listener;
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView name, price;

        public CarViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvCarName);
            price = itemView.findViewById(R.id.tvCarPrice);
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

        holder.name.setText(car.getName());
        holder.price.setText(car.getPrice());

        // 🔥 CLICK LISTENER (IMPORTANT PART)
        holder.itemView.setOnClickListener(v -> {
            listener.onItemClick(car);
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }
}