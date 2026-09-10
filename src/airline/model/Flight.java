package airline.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Flight {
    private final String id;
    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDateTime departureTime;
    private final List<Seat> seats;
    private final double basePrice;
    private final Set<String> bookedSeatIds = new HashSet<>();

    private Flight(Builder b) {
        this.id = b.id;
        this.flightNumber = b.flightNumber;
        this.origin = b.origin;
        this.destination = b.destination;
        this.departureTime = b.departureTime;
        this.seats = new ArrayList<>(b.seats);
        this.basePrice = b.basePrice;
    }

    public static Builder builder() { return new Builder(); }

    // Builder: seven positional args, four of them Strings -- swapping
    // origin/destination or id/flightNumber compiles fine and is only
    // caught at runtime. Named setters make the call site self-describing,
    // and build() enforces the invariants (non-empty seat map, positive
    // base price, origin != destination) in exactly one place.
    public static class Builder {
        private String id;
        private String flightNumber;
        private String origin;
        private String destination;
        private LocalDateTime departureTime;
        private List<Seat> seats = new ArrayList<>();
        private double basePrice;

        public Builder id(String id) { this.id = id; return this; }
        public Builder flightNumber(String flightNumber) { this.flightNumber = flightNumber; return this; }
        public Builder origin(String origin) { this.origin = origin; return this; }
        public Builder destination(String destination) { this.destination = destination; return this; }
        public Builder departureTime(LocalDateTime departureTime) { this.departureTime = departureTime; return this; }
        public Builder seats(List<Seat> seats) { this.seats = new ArrayList<>(seats); return this; }
        public Builder basePrice(double basePrice) { this.basePrice = basePrice; return this; }

        public Flight build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(flightNumber, "flightNumber is required");
            Objects.requireNonNull(origin, "origin is required");
            Objects.requireNonNull(destination, "destination is required");
            Objects.requireNonNull(departureTime, "departureTime is required");
            if (origin.equalsIgnoreCase(destination)) throw new IllegalArgumentException("origin and destination must differ");
            if (seats.isEmpty()) throw new IllegalArgumentException("a flight needs at least one seat");
            if (basePrice <= 0) throw new IllegalArgumentException("basePrice must be positive");
            return new Flight(this);
        }
    }

    public String getId() { return id; }
    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }
    public double getBasePrice() { return basePrice; }

    public List<Seat> getSeatsByClass(SeatClass seatClass) {
        return seats.stream().filter(s -> s.getSeatClass() == seatClass).collect(Collectors.toList());
    }

    public synchronized boolean isBooked(String seatId) { return bookedSeatIds.contains(seatId); }

    public synchronized boolean reserveSeat(Seat seat) {
        return bookedSeatIds.add(seat.getId());
    }

    public synchronized void releaseSeat(Seat seat) {
        bookedSeatIds.remove(seat.getId());
    }

    public synchronized int getBookedCountForClass(SeatClass seatClass) {
        return (int) getSeatsByClass(seatClass).stream().filter(s -> bookedSeatIds.contains(s.getId())).count();
    }

    @Override
    public String toString() { return flightNumber + " " + origin + "->" + destination + " @ " + departureTime; }
}
