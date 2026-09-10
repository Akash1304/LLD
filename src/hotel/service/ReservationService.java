package hotel.service;

import hotel.model.Guest;
import hotel.model.Reservation;
import hotel.model.RoomType;
import hotel.strategy.PricingStrategy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationService {
    Reservation bookRoom(Guest guest, RoomType type, LocalDate checkIn, LocalDate checkOut, PricingStrategy pricingStrategy) throws NoRoomAvailableException;
    Reservation checkIn(String reservationId) throws InvalidReservationStateException;
    Reservation checkOut(String reservationId) throws InvalidReservationStateException;
    Optional<Reservation> cancelReservation(String reservationId);
    List<Reservation> getReservationsForGuest(String guestId);

    class NoRoomAvailableException extends Exception {
        public NoRoomAvailableException(String message) { super(message); }
    }

    class InvalidReservationStateException extends Exception {
        public InvalidReservationStateException(String message) { super(message); }
    }
}
