package calendar.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Calendar {
    private final String id;
    private final User owner;
    private final String name;
    private final Set<String> eventIds = new HashSet<>();

    public Calendar(String id, User owner, String name) {
        this.id = id;
        this.owner = owner;
        this.name = name;
    }

    public String getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }

    // synchronized so concurrent create/delete calls touching this
    // calendar's event-id set (from InMemoryEventService) can't interleave
    // into a corrupted HashSet -- one lock per Calendar instance, not a
    // global lock shared by every calendar.
    public synchronized void addEvent(String eventId) { eventIds.add(eventId); }
    public synchronized void removeEvent(String eventId) { eventIds.remove(eventId); }
    public synchronized Set<String> getEventIds() { return new HashSet<>(eventIds); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calendar calendar = (Calendar) o;
        return Objects.equals(id, calendar.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Calendar{" + "id='" + id + '\'' + ", owner=" + owner + ", name='" + name + '\'' + '}';
    }
}

