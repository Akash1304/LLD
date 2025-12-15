package parkinglot.vehicle;

public class Truck extends Vehicle {
    public Truck(String licensNumber) {
        super(licensNumber, VehicleSize.LARGE);
    }
}