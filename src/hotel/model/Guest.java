package hotel.model;

import java.util.Objects;

public class Guest {
    private final String id;
    private final String name;

    public Guest(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest)) return false;
        return Objects.equals(id, ((Guest) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "Guest{" + id + ", " + name + '}'; }
}
