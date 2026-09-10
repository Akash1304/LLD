package airline.service;

import airline.model.Booking;
import airline.model.Passenger;
import airline.model.SeatClass;
import airline.strategy.PricingStrategy;
import airline.strategy.SeatAssignmentStrategy;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking bookSeat(String flightId, Passenger passenger, SeatClass seatClass, SeatAssignmentStrategy assignmentStrategy, PricingStrategy pricingStrategy) throws NoSeatAvailableException;
    Optional<Booking> cancelBooking(String bookingId);
    List<Booking> getBookingsForPassenger(String passportNumber);

    class NoSeatAvailableException extends Exception {
        public NoSeatAvailableException(String message) { super(message); }
    }
}
