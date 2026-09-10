package ridesharing.service;

import ridesharing.model.Location;
import ridesharing.model.Ride;
import ridesharing.model.Rider;
import ridesharing.strategy.FareStrategy;
import ridesharing.strategy.MatchingStrategy;

import java.util.List;
import java.util.Optional;

public interface RideService {
    Ride requestRide(Rider rider, Location pickup, Location dropoff, MatchingStrategy matchingStrategy) throws NoDriverAvailableException;
    Ride completeRide(String rideId, FareStrategy fareStrategy) throws InvalidRideStateException;
    Optional<Ride> cancelRide(String rideId);
    List<Ride> getRidesForRider(String riderId);

    class NoDriverAvailableException extends Exception {
        public NoDriverAvailableException(String message) { super(message); }
    }

    class InvalidRideStateException extends Exception {
        public InvalidRideStateException(String message) { super(message); }
    }
}
