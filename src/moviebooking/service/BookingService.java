package moviebooking.service;

import moviebooking.model.Booking;
import moviebooking.model.Seat;
import moviebooking.strategy.SeatSelectionStrategy;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    Booking bookSeats(String showId, String customerId, int numSeats, SeatSelectionStrategy strategy) throws NotEnoughSeatsException;
    Booking bookSpecificSeats(String showId, String customerId, List<Seat> requestedSeats) throws SeatUnavailableException;
    Optional<Booking> cancelBooking(String bookingId);
    List<Booking> getBookingsForCustomer(String customerId);

    class NotEnoughSeatsException extends Exception {
        public NotEnoughSeatsException(String message) { super(message); }
    }

    class SeatUnavailableException extends Exception {
        public SeatUnavailableException(String message) { super(message); }
    }
}
