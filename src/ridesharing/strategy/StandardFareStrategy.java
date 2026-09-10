package ridesharing.strategy;

import ridesharing.model.Location;

public class StandardFareStrategy implements FareStrategy {
    private static final double BASE_FARE = 2.5;
    private static final double RATE_PER_UNIT_DISTANCE = 1.5;

    @Override
    public double calculateFare(Location pickup, Location dropoff) {
        return BASE_FARE + pickup.distanceTo(dropoff) * RATE_PER_UNIT_DISTANCE;
    }
}
