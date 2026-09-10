package moviebooking.strategy;

import moviebooking.model.Seat;
import moviebooking.model.Show;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FirstAvailableSeatStrategy implements SeatSelectionStrategy {
    @Override
    public Optional<List<Seat>> selectSeats(Show show, List<Seat> allSeats, int count) {
        List<Seat> picked = new ArrayList<>();
        for (Seat seat : allSeats) {
            if (!show.isBooked(seat.getId())) {
                picked.add(seat);
                if (picked.size() == count) return Optional.of(picked);
            }
        }
        return Optional.empty();
    }
}
