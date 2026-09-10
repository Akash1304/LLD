package ridesharing.strategy;

import ridesharing.model.Driver;
import ridesharing.model.Location;

import java.util.List;
import java.util.Optional;

public interface MatchingStrategy {
    Optional<Driver> selectDriver(List<Driver> availableDrivers, Location pickup);
}
