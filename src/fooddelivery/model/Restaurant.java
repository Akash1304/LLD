package fooddelivery.model;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Restaurant {
    private final String id;
    private final String name;
    private final Location location;
    private final Map<String, MenuItem> menu = new ConcurrentHashMap<>();

    public Restaurant(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }

    public void addMenuItem(MenuItem item) { menu.put(item.getId(), item); }
    public Optional<MenuItem> getMenuItem(String itemId) { return Optional.ofNullable(menu.get(itemId)); }

    @Override
    public String toString() { return name + " @ " + location; }
}
