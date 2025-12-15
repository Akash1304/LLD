package parkinglot.vehicle;

public class Bike extends Vehicle {
    public Bike(String licensNumber) {
        super(licensNumber, VehicleSize.SMALL);
    }
}
