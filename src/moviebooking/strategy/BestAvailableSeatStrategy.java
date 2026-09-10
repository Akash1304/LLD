package moviebooking.strategy;

import moviebooking.model.Seat;
import moviebooking.model.Show;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

// Groups seats to have a group of friends/family sit together: looks for a
// contiguous run of `count` free seats within a single row (lowest row
// number first), falling back to any `count` free seats if no row has
// enough contiguous space.
public class BestAvailableSeatStrategy implements SeatSelectionStrategy {
    @Override
    public Optional<List<Seat>> selectSeats(Show show, List<Seat> allSeats, int count) {
        Map<Integer, List<Seat>> byRow = allSeats.stream().collect(Collectors.groupingBy(Seat::getRow, TreeMap::new, Collectors.toList()));

        for (List<Seat> rowSeats : byRow.values()) {
            rowSeats.sort(Comparator.comparingInt(Seat::getNumber));
            List<Seat> run = new ArrayList<>();
            for (Seat seat : rowSeats) {
                if (!show.isBooked(seat.getId())) {
                    run.add(seat);
                    if (run.size() == count) return Optional.of(new ArrayList<>(run));
                } else {
                    run.clear();
                }
            }
        }

        return new FirstAvailableSeatStrategy().selectSeats(show, allSeats, count);
    }
}
