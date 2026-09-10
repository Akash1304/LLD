package airline.service;

import airline.model.Booking;
import airline.model.Flight;
import airline.model.Passenger;
import airline.model.Seat;
import airline.model.SeatClass;
import airline.strategy.PricingStrategy;
import airline.strategy.SeatAssignmentStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryBookingService implements BookingService {
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final FlightService flightService;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryBookingService(FlightService flightService) {
        this.flightService = flightService;
    }

    // Locks on the specific Flight, not the whole service: assignSeat
    // (read) -> calculatePrice (read) -> reserveSeat (write) must be
    // atomic together for THIS flight (otherwise demand pricing could read
    // stale occupancy, or two bookings could both assign the same seat
    // before either reserves it), but a booking on a DIFFERENT flight has
    // no data in common and should never wait on this one. Unlike Parking
    // Lot/Movie Booking's optimistic-retry approach, a single lock for the
    // whole operation is the right call here because the read scan
    // (assignSeat) is already scoped to one flight, not spread across many
    // independently-lockable resources.
    @Override
    public Booking bookSeat(String flightId, Passenger passenger, SeatClass seatClass, SeatAssignmentStrategy assignmentStrategy, PricingStrategy pricingStrategy) throws NoSeatAvailableException {
        Flight flight = flightService.getFlight(flightId)
                .orElseThrow(() -> new NoSeatAvailableException("Unknown flight: " + flightId));

        synchronized (flight) {
            // price is computed BEFORE reserving the seat, so demand-based
            // pricing reflects occupancy at the moment of booking, not after
            Seat seat = assignmentStrategy.assignSeat(flight, seatClass)
                    .orElseThrow(() -> new NoSeatAvailableException("No available " + seatClass + " seat on flight " + flight.getFlightNumber()));
            double price = pricingStrategy.calculatePrice(flight, seat);

            if (!flight.reserveSeat(seat)) {
                throw new NoSeatAvailableException("Seat " + seat.getId() + " was just taken, please retry");
            }

            Booking booking = new Booking("BK-" + idCounter.getAndIncrement(), flight, passenger, seat, price);
            bookings.put(booking.getId(), booking);
            return booking;
        }
    }

    @Override
    public Optional<Booking> cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || !booking.tryCancel()) return Optional.empty();
        booking.getFlight().releaseSeat(booking.getSeat());
        return Optional.of(booking);
    }

    @Override
    public List<Booking> getBookingsForPassenger(String passportNumber) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getPassenger().getPassportNumber().equals(passportNumber)) result.add(b);
        }
        return result;
    }
}
