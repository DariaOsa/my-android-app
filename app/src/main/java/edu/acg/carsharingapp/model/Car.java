package edu.acg.carsharingapp.model;

import java.io.Serializable; // ✅ ADD THIS

public class Car implements Serializable { // ✅ ADD THIS

    private String name;
    private String price;
    private String description;
    private int imageResId;
    private int seats;
    private String fuelType;
    private String transmission;

    public Car(String name, String price, String description,
               int imageResId, int seats, String fuelType, String transmission) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageResId = imageResId;
        this.seats = seats;
        this.fuelType = fuelType;
        this.transmission = transmission;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getSeats() {
        return seats;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getTransmission() {
        return transmission;
    }
}
