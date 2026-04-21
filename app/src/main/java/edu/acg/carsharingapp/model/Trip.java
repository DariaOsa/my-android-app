package edu.acg.carsharingapp.model;

import java.util.HashMap;
import java.util.Map;

public class Trip {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String tripId;
    private String driverId;

    // 🚗 CAR SNAPSHOT
    private String carName;
    private double price;

    // 📍 ROUTE (TEXT)
    private String fromLocation;
    private String toLocation;

    // 📍 ROUTE (COORDINATES)
    private double fromLat;
    private double fromLng;
    private double toLat;
    private double toLng;

    // ⏰ TIME
    private String departureTime;

    private int availableSeats;
    private String status;
    private Map<String, Boolean> passengers;

    // 🔥 REQUIRED EMPTY CONSTRUCTOR (Firebase)
    public Trip() {}

    public Trip(String tripId, String driverId, String carName,
                double price, String fromLocation, String toLocation,
                String departureTime, int availableSeats) {

        this.tripId = tripId;
        this.driverId = driverId;
        this.carName = carName;
        this.price = price;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.status = STATUS_AVAILABLE;
        this.passengers = new HashMap<>();
    }

    // =========================
    // 📦 DISPLAY HELPERS
    // =========================

    public String getRoute() {
        if (fromLocation == null || toLocation == null) return "Unknown route";
        return fromLocation + " → " + toLocation;
    }

    public String getFormattedPrice() {
        return String.format("€%.2f", price);
    }

    public String getFormattedTime() {
        return departureTime != null ? departureTime : "Unknown time";
    }

    public String getSeatsText() {
        return availableSeats + " seats available";
    }

    public String getStatusDisplay() {
        switch (status) {
            case STATUS_AVAILABLE:
                return "AVAILABLE";
            case STATUS_IN_PROGRESS:
                return "IN PROGRESS";
            case STATUS_COMPLETED:
                return "COMPLETED";
            case STATUS_BOOKED:
                return "FULL";
            default:
                return "UNKNOWN";
        }
    }

    // 🔥 NEW: Used in Profile history
    public String getHistoryText() {
        return carName
                + " • €" + price
                + " • " + getFormattedTime();
    }

    // =========================
    // 📥 GETTERS
    // =========================

    public String getTripId() { return tripId; }
    public String getDriverId() { return driverId; }
    public String getCarName() { return carName; }
    public double getPrice() { return price; }
    public String getFromLocation() { return fromLocation; }
    public String getToLocation() { return toLocation; }
    public String getDepartureTime() { return departureTime; }
    public int getAvailableSeats() { return availableSeats; }
    public String getStatus() { return status; }

    public double getFromLat() { return fromLat; }
    public double getFromLng() { return fromLng; }
    public double getToLat() { return toLat; }
    public double getToLng() { return toLng; }

    public Map<String, Boolean> getPassengers() {
        if (passengers == null) passengers = new HashMap<>();
        return passengers;
    }

    // =========================
    // ✏️ SETTERS
    // =========================

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void setPassengers(Map<String, Boolean> passengers) {
        this.passengers = passengers;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFromLat(double fromLat) {
        this.fromLat = fromLat;
    }

    public void setFromLng(double fromLng) {
        this.fromLng = fromLng;
    }

    public void setToLat(double toLat) {
        this.toLat = toLat;
    }

    public void setToLng(double toLng) {
        this.toLng = toLng;
    }

    // =========================
    // 🧠 BUSINESS LOGIC
    // =========================

    public boolean hasPassenger(String userId) {
        return userId != null && getPassengers().containsKey(userId);
    }

    public boolean isDriver(String userId) {
        return userId != null && userId.equals(driverId);
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    public boolean isBooked() {
        return STATUS_BOOKED.equals(status);
    }

    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isFull() {
        return availableSeats <= 0;
    }

    public int getPassengerCount() {
        return getPassengers().size();
    }
}
