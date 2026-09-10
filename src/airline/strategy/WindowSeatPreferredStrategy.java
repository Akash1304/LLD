package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;
import airline.model.SeatClass;

import java.util.Comparator;
import java.util.Optional;

// Prefers a window seat if one is free; otherwise falls back to any
// available seat in the requested class.
public class WindowSeatPreferredStrategy implements SeatAssignmentStrategy {
    @Override
    public Optional<Seat> assignSeat(Flight flight, SeatClass seatClass) {
        return flight.getSeatsByClass(seatClass).stream()
                .filter(s -> !flight.isBooked(s.getId()))
                .max(Comparator.comparing(Seat::isWindow));
    }
}
