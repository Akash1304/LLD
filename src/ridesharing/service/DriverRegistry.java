package ridesharing.service;

import ridesharing.model.Driver;
import ridesharing.model.Location;

import java.util.List;
import java.util.Optional;

public interface DriverRegistry {
    Driver registerDriver(Driver driver);
    void updateLocation(String driverId, Location location);
    void setAvailability(String driverId, boolean available);
    List<Driver> getAvailableDrivers();
    Optional<Driver> getDriver(String driverId);
}
