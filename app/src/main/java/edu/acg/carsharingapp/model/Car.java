package edu.acg.carsharingapp.model;

import android.content.Context;

import java.io.Serializable;

import edu.acg.carsharingapp.R;

public class Car implements Serializable {

    private String brand;
    private String model;
    private String category;
    private double pricePerTrip;
    private float rating;
    private int imageResId;
    private int seats;

    // ✅ Resource IDs for localization
    private int fuelTypeResId;
    private int transmissionResId;

    public Car() {}

    public Car(String brand, String model, String category,
               double pricePerTrip, float rating,
               int imageResId, int seats,
               int fuelTypeResId, int transmissionResId) {

        this.brand = brand;
        this.model = model;
        this.category = category;
        this.pricePerTrip = pricePerTrip;
        this.rating = rating;
        this.imageResId = imageResId;
        this.seats = seats;
        this.fuelTypeResId = fuelTypeResId;
        this.transmissionResId = transmissionResId;
    }

    // =========================
    // 🧠 DISPLAY HELPERS (LOCALIZED)
    // =========================

    public String getDisplayName() {
        return brand + " " + model;
    }

    public String getFullName() {
        return brand + " " + model + " • " + category;
    }

    public String getSeatsText(Context context) {
        return context.getString(R.string.seats_available, seats);
    }

    public String getFuelDisplay(Context context) {
        return context.getString(fuelTypeResId);
    }

    public String getTransmissionDisplay(Context context) {
        return context.getString(transmissionResId);
    }

    public String getFormattedPrice(Context context) {
        return context.getString(R.string.price_per_km, pricePerTrip);
    }

    public String getFormattedRating(Context context) {
        return context.getString(R.string.rating_format, rating);
    }

    public String getMetaText(Context context) {
        return context.getString(R.string.car_meta, category, rating);
    }

    public String getSpecsText(Context context) {
        return context.getString(R.string.car_specs,
                getSeatsText(context),
                getFuelDisplay(context),
                getTransmissionDisplay(context)
        );
    }

    // =========================
    // 📥 GETTERS
    // =========================

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getCategory() { return category; }
    public double getPricePerTrip() { return pricePerTrip; }
    public float getRating() { return rating; }
    public int getImageResId() { return imageResId; }
    public int getSeats() { return seats; }
    public int getFuelTypeResId() { return fuelTypeResId; }
    public int getTransmissionResId() { return transmissionResId; }
}