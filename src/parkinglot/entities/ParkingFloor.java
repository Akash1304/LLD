package parkinglot.entities;

import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleSize;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ParkingFloor {
    private final int floorNumber;
    private final Map<String, ParkingSpot> spots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ConcurrentHashMap<>();
    }

    public void addSpot(ParkingSpot spot) {
        spots.put(spot.getSpotId(), spot);
    }

    // Deliberately NOT synchronized: this is a read-only, best-effort scan
    // over a ConcurrentHashMap (safe for concurrent iteration, no
    // ConcurrentModificationException) that returns only a *candidate*.
    // A floor-wide lock here would serialize every parking attempt on this
    // floor behind one monitor for no correctness benefit, since the actual
    // atomicity guarantee against double-booking lives in
    // ParkingSpot.tryPark(), scoped to the one spot being claimed. See
    // ParkingLot.parkVehicle() for the optimistic-retry loop this feeds.
    public Optional<ParkingSpot> findAvailableSpot(Vehicle vehicle) {
        return spots.values().stream()
                .filter(spot -> spot.canFitVehicle(vehicle))
                .sorted(Comparator.comparing(ParkingSpot::getSpotSize))
                .findFirst();
    }

    public long countAvailable(VehicleSize size) {
        return spots.values().stream().filter(s -> s.getSpotSize() == size && !s.isOccupied()).count();
    }

    public void displayAvailability() {
        System.out.printf("--- Floor %d Availability ---\n", floorNumber);
        Map<VehicleSize, Long> availableCounts = spots.values().stream()
                .filter(spot -> !spot.isOccupied())
                .collect(Collectors.groupingBy(ParkingSpot::getSpotSize, Collectors.counting()));

        for (VehicleSize size : VehicleSize.values()) {
            System.out.printf("  %s spots: %d\n", size, availableCounts.getOrDefault(size, 0L));
        }
    }

}
