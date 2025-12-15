package parkinglot.vehicle;

public class Car extends Vehicle {
    public Car(String licensNumber) {
        super(licensNumber, VehicleSize.MEDIUM);
    }
}
