package parkinglot.factory;

import parkinglot.vehicle.Bike;
import parkinglot.vehicle.Car;
import parkinglot.vehicle.Truck;
import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleType;

// Factory: the entry gate reads "CAR" off a ticket machine or a plate
// scanner -- a string/enum, not a class. This is the one place that maps
// type -> concrete Vehicle, so the gate, the driver, and any future API
// layer never `new Car(...)` themselves. Adding an ELECTRIC_CAR that needs
// a charging spot is one new case here plus one subclass; every caller is
// untouched.
public final class VehicleFactory {
    private VehicleFactory() {}

    public static Vehicle create(VehicleType type, String licenseNumber) {
        switch (type) {
            case BIKE:  return new Bike(licenseNumber);
            case CAR:   return new Car(licenseNumber);
            case TRUCK: return new Truck(licenseNumber);
            default:    throw new IllegalArgumentException("Unsupported vehicle type: " + type);
        }
    }
}
