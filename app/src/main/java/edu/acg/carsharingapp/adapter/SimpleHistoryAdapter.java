package edu.acg.carsharingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Trip;

public class SimpleHistoryAdapter extends RecyclerView.Adapter<SimpleHistoryAdapter.ViewHolder> {

    private final List<Trip> items;

    public SimpleHistoryAdapter(List<Trip> items) {
        this.items = items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtCar, txtPrice, txtRoute, txtDate, txtStatus;

        public ViewHolder(View itemView) {
            super(itemView);

            txtCar = itemView.findViewById(R.id.txtCarName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtRoute = itemView.findViewById(R.id.txtRoute);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Trip trip = items.get(position);
        var context = holder.itemView.getContext();

        // 🚗 CAR
        holder.txtCar.setText(trip.getCarName());

        // 💰 PRICE
        double price = trip.getFinalPrice() > 0
                ? trip.getFinalPrice()
                : trip.getPrice();

        holder.txtPrice.setText(
                context.getString(R.string.price_simple, price)
        );

        // 📍 ROUTE
        if (trip.getFromAddress() != null && trip.getToAddress() != null) {
            holder.txtRoute.setText(
                    context.getString(
                            R.string.route_format,
                            trip.getFromAddress(),
                            trip.getToAddress()
                    )
            );
        } else {
            holder.txtRoute.setText(context.getString(R.string.selected_location));
        }

        // 📅 DATE
        long time = trip.getCompletedAt() > 0
                ? trip.getCompletedAt()
                : trip.getCreatedAt();

        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(time));

        holder.txtDate.setText(date);

        // 🟢 STATUS + COLOR
        String status = trip.getStatus();

        if (Trip.STATUS_COMPLETED.equals(status)) {
            holder.txtStatus.setText(context.getString(R.string.status_completed));
            holder.txtStatus.setTextColor(0xFF4CAF50);

        } else if (Trip.STATUS_IN_PROGRESS.equals(status)) {
            holder.txtStatus.setText(context.getString(R.string.status_in_progress));
            holder.txtStatus.setTextColor(0xFFFF9800);

        } else {
            holder.txtStatus.setText(context.getString(R.string.status_available));
            holder.txtStatus.setTextColor(0xFF9E9E9E);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}