package parkinglot.observer;

import parkinglot.entities.ParkingFloor;
import parkinglot.entities.ParkingSpot;
import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleSize;

import java.util.List;

// The classic "display board at the entrance" follow-up. Before Observer,
// the driver called floor.displayAvailability() by hand after every park
// -- forget once and the board is stale. Now the board reacts to the lot.
public class DisplayBoard implements ParkingEventListener {
    private final List<ParkingFloor> floors;

    public DisplayBoard(List<ParkingFloor> floors) {
        this.floors = floors;
    }

    @Override
    public void onVehicleParked(ParkingSpot spot, Vehicle vehicle) {
        System.out.println("  [Display Board] " + spot.getSpotId() + " taken by " + vehicle.getLicenseNumber() + ". " + freeSummary());
    }

    @Override
    public void onVehicleUnparked(ParkingSpot spot, Vehicle vehicle) {
        System.out.println("  [Display Board] " + spot.getSpotId() + " freed by " + vehicle.getLicenseNumber() + ". " + freeSummary());
    }

    private String freeSummary() {
        StringBuilder sb = new StringBuilder("Free:");
        for (VehicleSize size : VehicleSize.values()) {
            long free = floors.stream().mapToLong(f -> f.countAvailable(size)).sum();
            sb.append(' ').append(size).append('=').append(free);
        }
        return sb.toString();
    }
}
