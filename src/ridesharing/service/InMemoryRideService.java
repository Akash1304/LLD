package ridesharing.service;

import ridesharing.model.Driver;
import ridesharing.model.Location;
import ridesharing.model.Ride;
import ridesharing.model.RideStatus;
import ridesharing.model.Rider;
import ridesharing.strategy.FareStrategy;
import ridesharing.strategy.MatchingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRideService implements RideService {
    private final Map<String, Ride> rides = new ConcurrentHashMap<>();
    private final DriverRegistry driverRegistry;
    private final AtomicLong idCounter = new AtomicLong(1);
    // matchingStrategy.selectDriver() is a lock-free read-only scan, so
    // its result is only a candidate; tryClaim() can still lose a race to
    // a concurrent request matched to the same driver. Optimistic retry,
    // same shape as ParkingLot.parkVehicle() and the Movie Booking LLD's
    // bookSeats().
    private static final int MAX_MATCH_ATTEMPTS = 5;

    public InMemoryRideService(DriverRegistry driverRegistry) {
        this.driverRegistry = driverRegistry;
    }

    @Override
    public Ride requestRide(Rider rider, Location pickup, Location dropoff, MatchingStrategy matchingStrategy) throws NoDriverAvailableException {
        for (int attempt = 0; attempt < MAX_MATCH_ATTEMPTS; attempt++) {
            List<Driver> available = driverRegistry.getAvailableDrivers();
            Driver driver = matchingStrategy.selectDriver(available, pickup)
                    .orElseThrow(() -> new NoDriverAvailableException("No available driver for pickup at " + pickup));

            if (driver.tryClaim()) {
                Ride ride = new Ride("RIDE-" + idCounter.getAndIncrement(), rider, driver, pickup, dropoff);
                rides.put(ride.getId(), ride);
                return ride;
            }
            // another request claimed this exact driver between the scan
            // and tryClaim() -- loop and re-match against fresh availability
        }
        throw new NoDriverAvailableException("Drivers for pickup at " + pickup + " kept getting claimed by concurrent requests, please retry");
    }

    @Override
    public Ride completeRide(String rideId, FareStrategy fareStrategy) throws InvalidRideStateException {
        Ride ride = requireRide(rideId);
        if (!ride.tryTransition(RideStatus.ONGOING, RideStatus.COMPLETED)) {
            throw new InvalidRideStateException("Cannot complete ride in state " + ride.getStatus());
        }
        ride.setFare(fareStrategy.calculateFare(ride.getPickup(), ride.getDropoff()));
        driverRegistry.updateLocation(ride.getDriver().getId(), ride.getDropoff());
        driverRegistry.setAvailability(ride.getDriver().getId(), true);
        return ride;
    }

    @Override
    public Optional<Ride> cancelRide(String rideId) {
        Ride ride = rides.get(rideId);
        if (ride == null || !ride.tryTransition(RideStatus.ONGOING, RideStatus.CANCELLED)) return Optional.empty();
        driverRegistry.setAvailability(ride.getDriver().getId(), true);
        return Optional.of(ride);
    }

    @Override
    public List<Ride> getRidesForRider(String riderId) {
        List<Ride> result = new ArrayList<>();
        for (Ride r : rides.values()) {
            if (r.getRider().getId().equals(riderId)) result.add(r);
        }
        return result;
    }

    private Ride requireRide(String rideId) throws InvalidRideStateException {
        Ride r = rides.get(rideId);
        if (r == null) throw new InvalidRideStateException("Unknown ride: " + rideId);
        return r;
    }
}
