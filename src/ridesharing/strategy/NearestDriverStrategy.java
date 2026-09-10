package ridesharing.strategy;

import ridesharing.model.Driver;
import ridesharing.model.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestDriverStrategy implements MatchingStrategy {
    @Override
    public Optional<Driver> selectDriver(List<Driver> availableDrivers, Location pickup) {
        return availableDrivers.stream()
                .min(Comparator.comparingDouble(d -> d.getLocation().distanceTo(pickup)));
    }
}
