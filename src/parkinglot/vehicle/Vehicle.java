package parkinglot.vehicle;

public abstract class Vehicle {
    private String licensNumber;
    private VehicleSize size;

    public Vehicle(String licensNumber, VehicleSize size) {
        this.licensNumber = licensNumber;
        this.size = size;
    }
    public String getLicenseNumber() {
        return licensNumber;
    }
    public VehicleSize getSize() {
        return size;
    }
}
