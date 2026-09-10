package ridesharing.strategy;

import ridesharing.model.Location;

public interface FareStrategy {
    double calculateFare(Location pickup, Location dropoff);
}
