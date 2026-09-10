package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;
import airline.model.SeatClass;

import java.util.Optional;

public class FirstAvailableSeatAssignmentStrategy implements SeatAssignmentStrategy {
    @Override
    public Optional<Seat> assignSeat(Flight flight, SeatClass seatClass) {
        return flight.getSeatsByClass(seatClass).stream()
                .filter(s -> !flight.isBooked(s.getId()))
                .findFirst();
    }
}
