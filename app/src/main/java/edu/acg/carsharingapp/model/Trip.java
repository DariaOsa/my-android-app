package edu.acg.carsharingapp.model;

import java.util.HashMap;
import java.util.Map;

public class Trip {

    // =========================
    // 🚦 STATUS CONSTANTS
    // =========================
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    // =========================
    // 📦 FIELDS
    // =========================
    private String tripId;
    private String driverId;
    private String carName;
    private int availableSeats;
    private String status;
    private Map<String, Boolean> passengers;

    // =========================
    // 🔥 REQUIRED EMPTY CONSTRUCTOR (Firebase)
    // =========================
    public Trip() {}

    // =========================
    // 🏗 CONSTRUCTOR
    // =========================
    public Trip(String tripId, String driverId, String carName, int availableSeats) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.carName = carName;
        this.availableSeats = availableSeats;
        this.status = STATUS_AVAILABLE; // default state
        this.passengers = new HashMap<>();
    }

    // =========================
    // 📥 GETTERS
    // =========================
    public String getTripId() {
        return tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getCarName() {
        return carName;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public String getStatus() {
        return status;
    }

    // 🔒 Null-safe passengers getter
    public Map<String, Boolean> getPassengers() {
        if (passengers == null) {
            passengers = new HashMap<>();
        }
        return passengers;
    }

    // =========================
    // 📤 SETTERS
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

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPassengers(Map<String, Boolean> passengers) {
        this.passengers = passengers;
    }

    // =========================
    // 🧠 HELPER METHODS (clean logic)
    // =========================
    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    public boolean isBooked() {
        return STATUS_BOOKED.equals(status);
    }

    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    public boolean hasPassenger(String userId) {
        return getPassengers().containsKey(userId);
    }
}