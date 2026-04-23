package edu.acg.carsharingapp.model;

import java.util.HashMap;
import java.util.Map;

public class Trip {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String tripId;

    // 👤 Driver
    private String driverId;

    // 🚗 Snapshot (UI info)
    private String carName;
    private double price;

    // 📍 CURRENT POSITION
    private double currentLat;
    private double currentLng;

    // 🎯 Destination
    private double toLat;
    private double toLng;

    // 🧭 ROUTE (Google Directions encoded polyline)
    private String routePolyline;

    // 📍 HUMAN-READABLE ADDRESSES (for UI/history)
    private String fromAddress;
    private String toAddress;

    // 👥 Passengers
    private Map<String, Boolean> passengers;

    private int availableSeats;

    // 🔄 State
    private String status;

    // ⏱️ Time
    private long createdAt;
    private long startedAt;
    private long completedAt;

    // 💰 Final price (after ride)
    private double finalPrice;

    // 🔥 Required for Firebase
    public Trip() {}

    // =========================
    // 🏗️ CONSTRUCTOR (AVAILABLE CAR)
    // =========================

    public Trip(String tripId, double lat, double lng, int seats) {
        this.tripId = tripId;
        this.currentLat = lat;
        this.currentLng = lng;
        this.availableSeats = seats;
        this.status = STATUS_AVAILABLE;
        this.passengers = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    // =========================
    // 🧠 BUSINESS RULES
    // =========================

    public boolean canStart(String userId) {
        return STATUS_AVAILABLE.equals(status) && driverId == null;
    }

    public boolean canJoin(String userId) {
        return STATUS_IN_PROGRESS.equals(status)
                && availableSeats > 0
                && driverId != null;
    }

    public boolean isDriver(String userId) {
        return userId != null && userId.equals(driverId);
    }

    public boolean hasPassenger(String userId) {
        return passengers != null && passengers.containsKey(userId);
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    // =========================
    // 📥 GETTERS
    // =========================

    public String getTripId() { return tripId; }
    public String getDriverId() { return driverId; }
    public String getCarName() { return carName; }
    public double getPrice() { return price; }
    public double getCurrentLat() { return currentLat; }
    public double getCurrentLng() { return currentLng; }
    public double getToLat() { return toLat; }
    public double getToLng() { return toLng; }

    public double getFinalPrice() {
        return finalPrice;
    }

    public String getRoutePolyline() { return routePolyline; }

    public String getFromAddress() { return fromAddress; }
    public String getToAddress() { return toAddress; }

    public int getAvailableSeats() { return availableSeats; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    public long getStartedAt() { return startedAt; }
    public long getCompletedAt() { return completedAt; }

    public Map<String, Boolean> getPassengers() {
        if (passengers == null) passengers = new HashMap<>();
        return passengers;
    }

    // =========================
    // ✏️ SETTERS
    // =========================

    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setCarName(String carName) { this.carName = carName; }
    public void setPrice(double price) { this.price = price; }
    public void setCurrentLat(double currentLat) { this.currentLat = currentLat; }
    public void setCurrentLng(double currentLng) { this.currentLng = currentLng; }
    public void setToLat(double toLat) { this.toLat = toLat; }
    public void setToLng(double toLng) { this.toLng = toLng; }

    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public void setStatus(String status) { this.status = status; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public void setPassengers(Map<String, Boolean> passengers) {
        this.passengers = passengers;
    }

    // =========================
    // 📦 UI HELPERS
    // =========================

    public String getSeatsText() {
        return availableSeats + " seats available";
    }

    public String getFormattedPrice() {
        return String.format("€%.2f", price);
    }

    public String getHistoryText() {

        String routePart = (fromAddress != null && toAddress != null)
                ? fromAddress + " → " + toAddress
                : carName;

        if (finalPrice > 0) {
            return routePart + " • €" + String.format("%.2f", finalPrice);
        } else {
            return routePart + " • €" + String.format("%.2f", price) + "/km";
        }
    }
}