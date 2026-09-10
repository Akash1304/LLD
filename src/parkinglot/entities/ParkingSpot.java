package parkinglot.entities;

import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleSize;

public class ParkingSpot {
    private final String spotId;
    private final VehicleSize spotSize;
    private boolean isOccupied;
    private Vehicle vehicle;

    public ParkingSpot(String spotId, VehicleSize size) {
        this.spotId = spotId;
        this.spotSize = size;
        this.isOccupied = false;
        this.vehicle = null;
    }

    public String getSpotId() {
        return spotId;
    }
    public VehicleSize getSpotSize() {
        return spotSize;
    }

    public synchronized  boolean isOccupied() {
        return isOccupied;
    }

    public synchronized void parkVehicle(Vehicle vehicle) {
        if (isOccupied) {
            throw new IllegalStateException("Parking spot is already occupied.");
        }
        if (vehicle.getSize().ordinal() > spotSize.ordinal()) {
            throw new IllegalArgumentException("Vehicle size exceeds parking spot size.");
        }
        this.vehicle = vehicle;
        this.isOccupied = true;
    }

    public synchronized void unparkVehicle() {
        this.vehicle = null;
        this.isOccupied = false;
    }

    // Atomic "claim if free" primitive: the check (occupied? fits?) and the
    // write (mark occupied) happen under the same monitor, so two threads
    // racing to park in this exact spot can never both succeed. Callers
    // that only had a *candidate* spot from a read-only scan (see
    // ParkingStrategy.findSpot) must use this instead of the two-step
    // canFitVehicle()-then-parkVehicle() sequence, which is a classic
    // check-then-act race under concurrent access.
    public synchronized boolean tryPark(Vehicle vehicle) {
        if (!canFitVehicle(vehicle)) return false;
        this.vehicle = vehicle;
        this.isOccupied = true;
        return true;
    }

    public synchronized boolean canFitVehicle(Vehicle vehicle) {
        if (isOccupied) return false;

        switch (vehicle.getSize()) {
            case SMALL:
                return spotSize == VehicleSize.SMALL;
            case MEDIUM:
                return spotSize == VehicleSize.MEDIUM || spotSize == VehicleSize.LARGE;
            case LARGE:
                return spotSize == VehicleSize.LARGE;
            default:
                return false;
        }
    }

}
