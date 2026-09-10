package parkinglot;

import parkinglot.entities.ParkingFloor;
import parkinglot.entities.ParkingSpot;
import parkinglot.entities.ParkingTicket;
import parkinglot.observer.ParkingEventListener;
import parkinglot.strategy.fee.FeeStrategy;
import parkinglot.strategy.fee.FlatRateFeeStrategy;
import parkinglot.strategy.parking.BestFitStrategy;
import parkinglot.strategy.parking.ParkingStrategy;
import parkinglot.vehicle.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParkingLot {
    // volatile is required for correct double-checked locking: without it, a
    // thread could observe a non-null `instance` that points to a
    // half-constructed ParkingLot, because the JVM is otherwise free to
    // reorder the constructor's writes past the assignment to `instance`.
    private static volatile ParkingLot instance;
    private final List<ParkingFloor> floors = new CopyOnWriteArrayList<>();
    private final Map<String, ParkingTicket> activeTickets;
    // volatile so a strategy swapped in on one thread (e.g. an admin
    // console) is guaranteed visible to parkVehicle() calls running on
    // other threads, not just eventually-consistent.
    private volatile FeeStrategy feeStrategy;
    private volatile ParkingStrategy parkingStrategy;
    // Observer subject: notified after a park/unpark commits, never while
    // a spot's monitor is held
    private final List<ParkingEventListener> listeners = new CopyOnWriteArrayList<>();


    // Singleton Pattern
    private ParkingLot() {
        this.feeStrategy = new FlatRateFeeStrategy();
        this.parkingStrategy = new BestFitStrategy();
        this.activeTickets = new ConcurrentHashMap<>();
    }

    // Double-checked locking: the common case (already initialized) reads
    // `instance` once, lock-free. Only the rare first-caller-ever path pays
    // for the synchronized block, and the second null-check inside it
    // prevents two threads that both saw null from constructing two lots.
    public static ParkingLot getInstance() {
        ParkingLot result = instance;
        if (result == null) {
            synchronized (ParkingLot.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ParkingLot();
                }
            }
        }
        return result;
    }

    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
    }

    public void setFeeStrategy (FeeStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
    }

    public void setParkingStrategy(ParkingStrategy parkingStrategy) {
        this.parkingStrategy = parkingStrategy;
    }

    public void addListener(ParkingEventListener listener) {
        listeners.add(listener);
    }

    // ParkingStrategy.findSpot() is a lock-free read-only scan across floors,
    // so the spot it returns is only a *candidate* -- another thread can
    // claim it between the scan and the park attempt. Rather than holding one
    // big lock over the whole find+park sequence (which would serialize every
    // parking operation lot-wide, even onto unrelated floors/spots),
    // we optimistically retry: re-run the scan and try the next candidate
    // whenever tryPark() loses the race on a specific spot. Contention is
    // therefore scoped to individual ParkingSpot monitors, not the lot.
    private static final int MAX_PARK_ATTEMPTS = 5;

    public Optional<ParkingTicket> parkVehicle(Vehicle vehicle) {
        for (int attempt = 0; attempt < MAX_PARK_ATTEMPTS; attempt++) {
            Optional<ParkingSpot> candidate = parkingStrategy.findSpot(floors, vehicle);
            if (candidate.isEmpty()) {
                System.out.println("No available spot for " + vehicle.getLicenseNumber());
                return Optional.empty();
            }

            ParkingSpot spot = candidate.get();
            if (spot.tryPark(vehicle)) {
                ParkingTicket ticket = new ParkingTicket(vehicle, spot);
                activeTickets.put(vehicle.getLicenseNumber(), ticket);
                System.out.printf("%s parked at %s. Ticket: %s\n", vehicle.getLicenseNumber(), spot.getSpotId(), ticket.getTicketId());
                for (ParkingEventListener l : listeners) l.onVehicleParked(spot, vehicle);
                return Optional.of(ticket);
            }
            // another thread claimed this exact spot between the scan and
            // tryPark() -- loop and re-scan for a fresh candidate
        }

        System.out.println("No available spot for " + vehicle.getLicenseNumber() + " (lost the race too many times, try again)");
        return Optional.empty();
    }

    public Optional<Double> unparkVehicle(String licenseNumber) {
        ParkingTicket ticket = activeTickets.remove(licenseNumber);
        if (ticket == null) {
            System.out.println("Ticket not found");
            return Optional.empty();
        }

        ticket.setExitTimestamp();
        ticket.getSpot().unparkVehicle();
        for (ParkingEventListener l : listeners) l.onVehicleUnparked(ticket.getSpot(), ticket.getVehicle());

        Double parkingFee = feeStrategy.calculateFee(ticket);

        return Optional.of(parkingFee);
    }
}
