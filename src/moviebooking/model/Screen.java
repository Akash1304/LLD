package moviebooking.model;

import java.util.Collections;
import java.util.List;

public class Screen {
    private final String id;
    private final String name;
    private final List<Seat> seats;

    public Screen(String id, String name, List<Seat> seats) {
        this.id = id;
        this.name = name;
        this.seats = seats;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }

    @Override
    public String toString() { return name + " (" + seats.size() + " seats)"; }
}
