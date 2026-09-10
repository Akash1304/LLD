package fooddelivery.model;

import java.util.Objects;

public class DeliveryAgent {
    private final String id;
    private final String name;
    private Location location;
    private boolean available;
    private int completedDeliveries = 0;

    public DeliveryAgent(String id, String name, Location location) {
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
    public synchronized int getCompletedDeliveries() { return completedDeliveries; }
    public synchronized void incrementCompletedDeliveries() { completedDeliveries++; }

    // Atomic "claim if available" -- one monitor per agent, so two orders
    // being assigned concurrently can't both claim the same agent (a race
    // in the read-only assignment-strategy scan that picks a candidate).
    public synchronized boolean tryClaim() {
        if (!available) return false;
        available = false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryAgent)) return false;
        return Objects.equals(id, ((DeliveryAgent) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return name + "@" + location + (available ? "" : " (busy)"); }
}
