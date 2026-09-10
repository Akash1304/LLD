package parkinglot.interview;

import java.util.*;

// Compact, single-file interview-friendly parking lot demo.
// Supports: multi-floor spots sized SMALL/MEDIUM/LARGE, best-fit spot
// assignment, ticket issuance, and hourly fee calculation on exit.
public class SimpleParkingLotInterview {

    enum Size { SMALL, MEDIUM, LARGE }

    static class Vehicle {
        final String license;
        final Size size;
        Vehicle(String license, Size size) { this.license = license; this.size = size; }
    }

    static class Spot {
        final String id;
        final int floor;
        final Size size;
        boolean occupied = false;
        Spot(String id, int floor, Size size) { this.id = id; this.floor = floor; this.size = size; }
        boolean canFit(Vehicle v) { return !occupied && v.size.ordinal() <= size.ordinal(); }
        @Override public String toString() { return id + "(" + size + (occupied ? ",occupied" : ",free") + ")"; }
    }

    static class Ticket {
        final Vehicle vehicle;
        final Spot spot;
        final long entryMillis;
        long exitMillis = -1;
        Ticket(Vehicle vehicle, Spot spot, long entryMillis) {
            this.vehicle = vehicle; this.spot = spot; this.entryMillis = entryMillis;
        }
    }

    // in-memory store for one lot
    final List<Spot> spots = new ArrayList<>();
    final Map<String, Ticket> activeTickets = new HashMap<>();
    final Map<Size, Double> hourlyRate = Map.of(Size.SMALL, 10.0, Size.MEDIUM, 20.0, Size.LARGE, 30.0);

    void addSpot(Spot spot) { spots.add(spot); }

    // best-fit: among all free spots that fit the vehicle, pick the smallest one
    Optional<Spot> findSpot(Vehicle vehicle) {
        return spots.stream()
                .filter(s -> s.canFit(vehicle))
                .min(Comparator.comparingInt(s -> s.size.ordinal()));
    }

    Optional<Ticket> park(Vehicle vehicle) {
        Optional<Spot> spotOpt = findSpot(vehicle);
        if (spotOpt.isEmpty()) {
            System.out.println("No spot available for " + vehicle.license + " (" + vehicle.size + ")");
            return Optional.empty();
        }
        Spot spot = spotOpt.get();
        spot.occupied = true;
        Ticket ticket = new Ticket(vehicle, spot, System.currentTimeMillis());
        activeTickets.put(vehicle.license, ticket);
        System.out.println(vehicle.license + " parked at " + spot.id + " (floor " + spot.floor + ")");
        return Optional.of(ticket);
    }

    // exitMillis is passed in explicitly so the demo can simulate elapsed time
    Optional<Double> unpark(String license, long exitMillis) {
        Ticket ticket = activeTickets.remove(license);
        if (ticket == null) {
            System.out.println("No active ticket for " + license);
            return Optional.empty();
        }
        ticket.exitMillis = exitMillis;
        ticket.spot.occupied = false;
        long elapsedMillis = ticket.exitMillis - ticket.entryMillis;
        long hours = elapsedMillis / (1000 * 60 * 60) + 1; // any partial hour rounds up
        double fee = hours * hourlyRate.get(ticket.vehicle.size);
        return Optional.of(fee);
    }

    void printAvailability() {
        Map<Integer, List<Spot>> byFloor = new TreeMap<>();
        for (Spot s : spots) byFloor.computeIfAbsent(s.floor, f -> new ArrayList<>()).add(s);
        byFloor.forEach((floor, floorSpots) -> System.out.println("Floor " + floor + ": " + floorSpots));
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleParkingLotInterview lot = new SimpleParkingLotInterview();
        System.out.println("== Simple Parking Lot Interview Demo ==");

        lot.addSpot(new Spot("F1-S1", 1, Size.SMALL));
        lot.addSpot(new Spot("F1-M1", 1, Size.MEDIUM));
        lot.addSpot(new Spot("F1-L1", 1, Size.LARGE));
        lot.addSpot(new Spot("F2-M1", 2, Size.MEDIUM));

        System.out.println("Initial availability:");
        lot.printAvailability();

        Vehicle bike = new Vehicle("B-1", Size.SMALL);
        Vehicle car1 = new Vehicle("C-1", Size.MEDIUM);
        Vehicle car2 = new Vehicle("C-2", Size.MEDIUM);
        Vehicle truck = new Vehicle("T-1", Size.LARGE);

        lot.park(bike);
        lot.park(car1);
        lot.park(truck);
        lot.park(car2); // floor 1's MEDIUM spot is taken, should overflow to floor 2

        System.out.println("\nTrying to park a second bike (should fail, no SMALL spots left):");
        lot.park(new Vehicle("B-2", Size.SMALL));

        System.out.println("\nAvailability after parking:");
        lot.printAvailability();

        System.out.println("\nUnparking " + car1.license + " after ~2.5 hours:");
        long twoAndHalfHoursMillis = (long) (2.5 * 60 * 60 * 1000);
        Optional<Double> fee = lot.unpark(car1.license, System.currentTimeMillis() + twoAndHalfHoursMillis);
        fee.ifPresent(f -> System.out.printf("Fee: $%.2f%n", f));

        System.out.println("\nFinal availability:");
        lot.printAvailability();
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}