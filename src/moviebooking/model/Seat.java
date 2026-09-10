package moviebooking.model;

import java.util.Objects;

public class Seat {
    private final String id;
    private final int row;
    private final int number;
    private final SeatType type;

    public Seat(String id, int row, int number, SeatType type) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.type = type;
    }

    public String getId() { return id; }
    public int getRow() { return row; }
    public int getNumber() { return number; }
    public SeatType getType() { return type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat)) return false;
        return Objects.equals(id, ((Seat) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id + "(" + type + ")"; }
}
