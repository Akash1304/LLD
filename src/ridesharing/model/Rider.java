package ridesharing.model;

import java.util.Objects;

public class Rider {
    private final String id;
    private final String name;

    public Rider(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rider)) return false;
        return Objects.equals(id, ((Rider) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "Rider{" + id + ", " + name + '}'; }
}
