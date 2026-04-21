package edu.acg.carsharingapp.model;

import java.io.Serializable;

public class Car implements Serializable {

    private String brand;
    private String model;
    private String category;
    private double pricePerTrip;
    private float rating;
    private int imageResId;
    private int seats;
    private String fuelType;
    private String transmission;

    // 🔗 Trip connection
    private String tripId;

    // 📍 Location
    private double latitude;
    private double longitude;

    // 📏 OPTIONAL: cached distance (useful for sorting later)
    private float distanceKm;

    // 🔥 REQUIRED (Firebase / Serialization)
    public Car() {}

    public Car(String brand, String model, String category,
               double pricePerTrip, float rating,
               int imageResId, int seats,
               String fuelType, String transmission) {

        this.brand = brand;
        this.model = model;
        this.category = category;
        this.pricePerTrip = pricePerTrip;
        this.rating = rating;
        this.imageResId = imageResId;
        this.seats = seats;
        this.fuelType = fuelType;
        this.transmission = transmission;
    }

    // =========================
    // 🎨 DISPLAY HELPERS
    // =========================

    public String getDisplayName() {
        return brand + " " + model;
    }

    public String getFullName() {
        return brand + " " + model + " • " + category;
    }

    public String getFormattedPrice() {
        return String.format("€%.2f / trip", pricePerTrip);
    }

    public String getFormattedRating() {
        return "⭐ " + rating;
    }

    public String getCategoryWithRating() {
        return category + " • ⭐ " + rating;
    }

    public String getSeatsText() {
        return seats + " seats";
    }

    public String getShortTransmission() {
        if (transmission == null) return "";
        return transmission.equalsIgnoreCase("Automatic") ? "Auto" : "Manual";
    }

    public String getFuelDisplay() {
        return fuelType != null ? fuelType : "Unknown";
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

    public String getFuelType() { return fuelType; }

    public String getTransmission() { return transmission; }

    // =========================
    // 📍 LOCATION
    // =========================

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // =========================
    // 📏 DISTANCE (OPTIONAL)
    // =========================

    public float getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(float distanceKm) {
        this.distanceKm = distanceKm;
    }

    // =========================
    // 🔗 TRIP ID
    // =========================

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }
}