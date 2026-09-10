package airline.model;

import java.util.Objects;

public class Seat {
    private final String id;
    private final int row;
    private final char column;
    private final SeatClass seatClass;

    public Seat(String id, int row, char column, SeatClass seatClass) {
        this.id = id;
        this.row = row;
        this.column = column;
        this.seatClass = seatClass;
    }

    public String getId() { return id; }
    public int getRow() { return row; }
    public char getColumn() { return column; }
    public SeatClass getSeatClass() { return seatClass; }
    public boolean isWindow() { return column == 'A' || column == 'F'; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat)) return false;
        return Objects.equals(id, ((Seat) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return id + "(" + seatClass + (isWindow() ? ",window" : "") + ")"; }
}
