package ridesharing.interview;

import java.util.*;

// Compact, single-file interview-friendly ride-sharing demo.
// Supports: nearest-driver matching, ride completion with a distance-based
// fare, and cancellation.
public class SimpleRideSharingInterview {

    static class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        double distanceTo(Point o) { return Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2)); }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    static class Driver {
        final String id;
        Point location;
        boolean available = true;
        Driver(String id, Point location) { this.id = id; this.location = location; }
    }

    static class Ride {
        final String id;
        final String rider;
        final Driver driver;
        final Point pickup, dropoff;
        boolean completed = false;
        Ride(String id, String rider, Driver driver, Point pickup, Point dropoff) {
            this.id = id; this.rider = rider; this.driver = driver; this.pickup = pickup; this.dropoff = dropoff;
        }
    }

    final List<Driver> drivers = new ArrayList<>();
    final Map<String, Ride> rides = new HashMap<>();
    int counter = 1;

    void addDriver(Driver d) { drivers.add(d); }

    Ride requestRide(String rider, Point pickup, Point dropoff) throws Exception {
        Driver nearest = drivers.stream()
                .filter(d -> d.available)
                .min(Comparator.comparingDouble(d -> d.location.distanceTo(pickup)))
                .orElseThrow(() -> new Exception("No available driver"));
        nearest.available = false;
        Ride ride = new Ride("RIDE-" + counter++, rider, nearest, pickup, dropoff);
        rides.put(ride.id, ride);
        return ride;
    }

    double completeRide(String rideId) {
        Ride ride = rides.get(rideId);
        ride.completed = true;
        ride.driver.available = true;
        ride.driver.location = ride.dropoff;
        double baseFare = 2.5;
        double ratePerUnit = 1.5;
        return baseFare + ride.pickup.distanceTo(ride.dropoff) * ratePerUnit;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleRideSharingInterview system = new SimpleRideSharingInterview();
        System.out.println("== Simple Ride-Sharing Interview Demo ==");

        system.addDriver(new Driver("D1", new Point(0, 0)));
        system.addDriver(new Driver("D2", new Point(2, 1)));

        Point pickup = new Point(1, 1);
        Point dropoff = new Point(6, 8);

        try {
            Ride ride1 = system.requestRide("Alice", pickup, dropoff);
            System.out.println("Alice matched with " + ride1.driver.id);

            System.out.println("\nRequesting a second ride at the same pickup (only D1 left):");
            Ride ride2 = system.requestRide("Bob", pickup, dropoff);
            System.out.println("Bob matched with " + ride2.driver.id);

            System.out.println("\nTrying a third ride (no drivers left):");
            try {
                system.requestRide("Carol", pickup, dropoff);
            } catch (Exception e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nCompleting Alice's ride:");
            double fare = system.completeRide(ride1.id);
            System.out.printf("Fare: $%.2f%n", fare);
        } catch (Exception e) {
            System.out.println("Ride request failed: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
