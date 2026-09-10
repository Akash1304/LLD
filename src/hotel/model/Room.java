package hotel.model;

import java.util.Objects;

public class Room {
    private final String id;
    private final RoomType type;
    private final int floor;

    public Room(String id, RoomType type, int floor) {
        this.id = id;
        this.type = type;
        this.floor = floor;
    }

    public String getId() { return id; }
    public RoomType getType() { return type; }
    public int getFloor() { return floor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        return Objects.equals(id, ((Room) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id + "(" + type + ", floor " + floor + ")"; }
}
