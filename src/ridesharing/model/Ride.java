package ridesharing.model;

public class Ride {
    private final String id;
    private final Rider rider;
    private final Driver driver;
    private final Location pickup;
    private final Location dropoff;
    private RideStatus status;
    private double fare;

    public Ride(String id, Rider rider, Driver driver, Location pickup, Location dropoff) {
        this.id = id;
        this.rider = rider;
        this.driver = driver;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.status = RideStatus.ONGOING;
    }

    public String getId() { return id; }
    public Rider getRider() { return rider; }
    public Driver getDriver() { return driver; }
    public Location getPickup() { return pickup; }
    public Location getDropoff() { return dropoff; }
    public synchronized RideStatus getStatus() { return status; }
    public synchronized double getFare() { return fare; }
    public synchronized void setFare(double fare) { this.fare = fare; }

    // Compare-and-set for status: completeRide/cancelRide both go through
    // this so a completion racing a cancellation on the same ride (e.g.
    // two double-submitted requests) can't both succeed.
    public synchronized boolean tryTransition(RideStatus expected, RideStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }

    @Override
    public String toString() {
        return "Ride{" + id + ", " + rider.getName() + " w/ " + driver.getName()
                + ", " + pickup + "->" + dropoff + ", " + status
                + (fare > 0 ? ", fare=$" + String.format("%.2f", fare) : "") + '}';
    }
}
