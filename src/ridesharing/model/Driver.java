package ridesharing.model;

import java.util.Objects;

public class Driver {
    private final String id;
    private final String name;
    private Location location;
    private boolean available;

    public Driver(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.available = true;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public synchronized Location getLocation() { return location; }
    public synchronized void setLocation(Location location) { this.location = location; }
    public synchronized boolean isAvailable() { return available; }
    public synchronized void setAvailable(boolean available) { this.available = available; }

    // Atomic "claim if available" -- the availability check and the flip
    // to unavailable happen under this driver's own monitor, so two
    // concurrent ride requests matched to the same driver (a race in the
    // read-only matching scan) can't both succeed in claiming them.
    public synchronized boolean tryClaim() {
        if (!available) return false;
        available = false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Driver)) return false;
        return Objects.equals(id, ((Driver) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "Driver{" + id + ", " + name + ", " + location + ", available=" + available + '}'; }
}
