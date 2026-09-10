package ridesharing.driver;

import ridesharing.model.Driver;
import ridesharing.model.Location;
import ridesharing.model.Ride;
import ridesharing.model.Rider;
import ridesharing.service.DriverRegistry;
import ridesharing.service.InMemoryDriverRegistry;
import ridesharing.service.InMemoryRideService;
import ridesharing.service.RideService;
import ridesharing.strategy.NearestDriverStrategy;
import ridesharing.strategy.StandardFareStrategy;
import ridesharing.strategy.SurgePricingFareStrategy;

public class RideSharingDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        DriverRegistry driverRegistry = new InMemoryDriverRegistry();
        RideService rideService = new InMemoryRideService(driverRegistry);

        driverRegistry.registerDriver(new Driver("D1", "Sam", new Location(0, 0)));
        driverRegistry.registerDriver(new Driver("D2", "Nina", new Location(10, 10)));
        driverRegistry.registerDriver(new Driver("D3", "Leo", new Location(2, 1)));

        Rider alice = new Rider("R1", "Alice");
        Location pickup = new Location(1, 1);
        Location dropoff = new Location(6, 8);

        try {
            System.out.println("Requesting a ride near (1,1) -- nearest driver should be Leo at (2,1):");
            Ride ride = rideService.requestRide(alice, pickup, dropoff, new NearestDriverStrategy());
            System.out.println(ride);

            System.out.println("\nAvailable drivers now: " + driverRegistry.getAvailableDrivers());

            System.out.println("\nRequesting a second ride at the same pickup -- Leo is busy, Sam is next nearest:");
            Ride ride2 = rideService.requestRide(new Rider("R2", "Bob"), pickup, dropoff, new NearestDriverStrategy());
            System.out.println(ride2);

            System.out.println("\nCompleting Alice's ride with surge pricing (1.5x) applied:");
            Ride completed = rideService.completeRide(ride.getId(), new SurgePricingFareStrategy(new StandardFareStrategy(), 1.5));
            System.out.println(completed);

            System.out.println("\nLeo is available again: " + driverRegistry.getAvailableDrivers());

            System.out.println("\nRequesting a third ride, standard pricing this time:");
            Ride ride3 = rideService.requestRide(new Rider("R3", "Carol"), pickup, dropoff, new NearestDriverStrategy());
            System.out.println(ride3);
            Ride completed3 = rideService.completeRide(ride3.getId(), new StandardFareStrategy());
            System.out.println(completed3);
        } catch (RideService.NoDriverAvailableException | RideService.InvalidRideStateException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
