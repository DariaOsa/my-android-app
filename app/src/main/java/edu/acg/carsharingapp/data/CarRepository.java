package edu.acg.carsharingapp.data;

import java.util.ArrayList;
import java.util.List;

import edu.acg.carsharingapp.R;
import edu.acg.carsharingapp.model.Car;

public class CarRepository {

    private static final List<Car> cars = new ArrayList<>();

    static {
        cars.add(new Car(
                "BMW",
                "3 Series",
                "Premium",
                12.0,
                4.6f,
                R.drawable.car2,
                5,
                "Electric",
                "Automatic"
        ));

        cars.add(new Car(
                "Audi",
                "A4",
                "Comfort",
                11.0,
                4.5f,
                R.drawable.car4,
                5,
                "Petrol",
                "Automatic"
        ));

        cars.add(new Car(
                "Toyota",
                "RAV4",
                "SUV",
                10.0,
                4.4f,
                R.drawable.car3,
                5,
                "Hybrid",
                "Automatic"
        ));

        cars.add(new Car(
                "Volkswagen",
                "Golf",
                "Economy",
                8.0,
                4.2f,
                R.drawable.car1,
                5,
                "Petrol",
                "Manual"
        ));

        cars.add(new Car(
                "Tesla",
                "Model 3",
                "Premium",
                15.0,
                4.8f,
                R.drawable.car5,
                5,
                "Electric",
                "Automatic"
        ));
    }

    // ✅ Get all cars
    public static List<Car> getCars() {
        return cars;
    }

    // ✅ Find car by display name (used in BookingActivity)
    public static Car getCarByName(String name) {
        if (name == null) return null;

        for (Car car : cars) {

            String fullName = car.getBrand() + " " + car.getModel();
            String fullWithCategory = fullName + " • " + car.getCategory();

            if (name.equals(fullName) || name.equals(fullWithCategory)) {
                return car;
            }
        }

        return null;
    }
}