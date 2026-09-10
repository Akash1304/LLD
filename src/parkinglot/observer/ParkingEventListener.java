package parkinglot.observer;

import parkinglot.entities.ParkingSpot;
import parkinglot.vehicle.Vehicle;

// Observer contract: the lot announces occupancy changes; display boards,
// a revenue counter, a "lot full" sign at the entrance can all subscribe
// without ParkingLot knowing any of them exist.
public interface ParkingEventListener {
    void onVehicleParked(ParkingSpot spot, Vehicle vehicle);
    void onVehicleUnparked(ParkingSpot spot, Vehicle vehicle);
}
