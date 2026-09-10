package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;
import airline.model.SeatClass;

import java.util.Optional;

public interface SeatAssignmentStrategy {
    Optional<Seat> assignSeat(Flight flight, SeatClass seatClass);
}
