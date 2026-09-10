package moviebooking.service;

import moviebooking.model.Booking;
import moviebooking.model.Seat;
import moviebooking.model.Show;
import moviebooking.strategy.SeatSelectionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryBookingService implements BookingService {
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final CatalogService catalogService;
    private final AtomicLong idCounter = new AtomicLong(1);
    // strategy.selectSeats() is a lock-free read-only scan, so its result
    // is only a candidate; show.reserveSeats() can still lose a race on
    // one of those seats to a concurrent booking. Rather than surfacing
    // that as a caller-visible failure on the first attempt, retry with a
    // fresh selection a bounded number of times -- the same optimistic
    // retry shape as ParkingLot.parkVehicle().
    private static final int MAX_BOOKING_ATTEMPTS = 5;

    public InMemoryBookingService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public Booking bookSeats(String showId, String customerId, int numSeats, SeatSelectionStrategy strategy) throws NotEnoughSeatsException {
        Show show = catalogService.getShow(showId).orElseThrow(() -> new NotEnoughSeatsException("Unknown show: " + showId));
        List<Seat> allSeats = show.getScreen().getSeats();

        for (int attempt = 0; attempt < MAX_BOOKING_ATTEMPTS; attempt++) {
            Optional<List<Seat>> selected = strategy.selectSeats(show, allSeats, numSeats);
            if (selected.isEmpty()) {
                throw new NotEnoughSeatsException("Could not find " + numSeats + " available seat(s) for show " + showId);
            }
            List<Seat> seats = selected.get();
            if (show.reserveSeats(seats)) {
                return createBooking(show, customerId, seats);
            }
            // another booking grabbed one of these seats between selection
            // and reservation -- loop and re-select with fresh availability
        }
        throw new NotEnoughSeatsException("Seats for show " + showId + " kept getting claimed by concurrent bookings, please retry");
    }

    @Override
    public Booking bookSpecificSeats(String showId, String customerId, List<Seat> requestedSeats) throws SeatUnavailableException {
        Show show = catalogService.getShow(showId).orElseThrow(() -> new SeatUnavailableException("Unknown show: " + showId));
        if (!show.reserveSeats(requestedSeats)) {
            throw new SeatUnavailableException("One or more requested seats are already booked");
        }
        return createBooking(show, customerId, requestedSeats);
    }

    private Booking createBooking(Show show, String customerId, List<Seat> seats) {
        double total = seats.stream().mapToDouble(s -> show.getBasePrice() * s.getType().getPriceMultiplier()).sum();
        Booking booking = new Booking("BK-" + idCounter.getAndIncrement(), show, customerId, seats, total);
        bookings.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || !booking.tryCancel()) return Optional.empty();
        booking.getShow().releaseSeats(booking.getSeats());
        return Optional.of(booking);
    }

    @Override
    public List<Booking> getBookingsForCustomer(String customerId) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings.values()) {
            if (b.getCustomerId().equals(customerId)) result.add(b);
        }
        return result;
    }
}
