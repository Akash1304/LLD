package parkinglot;

import parkinglot.entities.ParkingFloor;
import parkinglot.entities.ParkingSpot;
import parkinglot.entities.ParkingTicket;
import parkinglot.factory.VehicleFactory;
import parkinglot.observer.DisplayBoard;
import parkinglot.strategy.fee.VehicleBasedFeeStrategy;
import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleSize;
import parkinglot.vehicle.VehicleType;

import java.util.Arrays;
import java.util.Optional;

public class ParkingLotDriver {
    public static void main(String[] args) {
        driver();
    }

    public static void driver() {
        ParkingLot parkingLot = ParkingLot.getInstance();

        // 1. Initialize the parking lot with floors and spots
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new ParkingSpot("F1-S1", VehicleSize.SMALL));
        floor1.addSpot(new ParkingSpot("F1-M1", VehicleSize.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-L1", VehicleSize.LARGE));

        ParkingFloor floor2 = new ParkingFloor(2);
        floor2.addSpot(new ParkingSpot("F2-M1", VehicleSize.MEDIUM));
        floor2.addSpot(new ParkingSpot("F2-M2", VehicleSize.MEDIUM));

        parkingLot.addFloor(floor1);
        parkingLot.addFloor(floor2);
        parkingLot.setFeeStrategy(new VehicleBasedFeeStrategy());

        // Observer: the entrance display board subscribes once and updates
        // itself on every park/unpark -- nobody calls it by hand
        parkingLot.addListener(new DisplayBoard(Arrays.asList(floor1, floor2)));

        System.out.println("\n--- Vehicle Entries ---");
        floor1.displayAvailability();
        floor2.displayAvailability();

        // Factory: the gate only knows the vehicle TYPE off the ticket
        // machine, never a concrete class
        Vehicle bike = VehicleFactory.create(VehicleType.BIKE, "B-123");
        Vehicle car = VehicleFactory.create(VehicleType.CAR, "C-456");
        Vehicle truck = VehicleFactory.create(VehicleType.TRUCK, "T-789");

        parkingLot.parkVehicle(bike);
        Optional<ParkingTicket> carTicketOpt = parkingLot.parkVehicle(car);
        parkingLot.parkVehicle(truck);

        // 3. Another car entry (should go to floor 2)
        parkingLot.parkVehicle(VehicleFactory.create(VehicleType.CAR, "C-999"));

        // 4. A vehicle entry that fails (no available spots)
        parkingLot.parkVehicle(VehicleFactory.create(VehicleType.BIKE, "B-000"));

        // 5. Vehicle exit and fee calculation
        System.out.println("\n--- Vehicle Exits ---");
        if (carTicketOpt.isPresent()) {
            Optional<Double> feeOpt = parkingLot.unparkVehicle(car.getLicenseNumber());
            feeOpt.ifPresent(fee -> System.out.printf("Car C-456 unparked. Fee: $%.2f\n", fee));
        }
    }
}
