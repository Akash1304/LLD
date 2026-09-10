package moviebooking.strategy;

import moviebooking.model.Seat;
import moviebooking.model.Show;

import java.util.List;
import java.util.Optional;

public interface SeatSelectionStrategy {
    Optional<List<Seat>> selectSeats(Show show, List<Seat> allSeats, int count);
}
