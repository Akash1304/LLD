package ridesharing.service;

import ridesharing.model.Driver;
import ridesharing.model.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDriverRegistry implements DriverRegistry {
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    @Override
    public Driver registerDriver(Driver driver) {
        drivers.put(driver.getId(), driver);
        return driver;
    }

    @Override
    public void updateLocation(String driverId, Location location) {
        requireDriver(driverId).setLocation(location);
    }

    @Override
    public void setAvailability(String driverId, boolean available) {
        requireDriver(driverId).setAvailable(available);
    }

    @Override
    public List<Driver> getAvailableDrivers() {
        return drivers.values().stream().filter(Driver::isAvailable).collect(Collectors.toList());
    }

    @Override
    public Optional<Driver> getDriver(String driverId) {
        return Optional.ofNullable(drivers.get(driverId));
    }

    private Driver requireDriver(String driverId) {
        Driver d = drivers.get(driverId);
        if (d == null) throw new IllegalArgumentException("Unknown driver: " + driverId);
        return d;
    }
}
