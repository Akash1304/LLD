package moviebooking.interview;

import java.util.*;

// Compact, single-file interview-friendly movie ticket booking demo.
// Supports: a single show with a grid of seats, best-available (contiguous)
// seat selection, booking, and cancellation.
public class SimpleMovieBookingInterview {

    static class Seat {
        final String id;
        final int row;
        final int num;
        boolean booked = false;
        Seat(String id, int row, int num) { this.id = id; this.row = row; this.num = num; }
        @Override public String toString() { return id; }
    }

    static class Booking {
        final String id;
        final String customer;
        final List<Seat> seats;
        boolean cancelled = false;
        Booking(String id, String customer, List<Seat> seats) { this.id = id; this.customer = customer; this.seats = seats; }
    }

    final List<Seat> seats = new ArrayList<>();
    final Map<String, Booking> bookings = new HashMap<>();
    int bookingCounter = 1;

    void addSeat(Seat s) { seats.add(s); }

    // best-available: first contiguous run of `count` free seats in the same row
    Optional<List<Seat>> findBestAvailable(int count) {
        Map<Integer, List<Seat>> byRow = new TreeMap<>();
        for (Seat s : seats) byRow.computeIfAbsent(s.row, r -> new ArrayList<>()).add(s);

        for (List<Seat> rowSeats : byRow.values()) {
            rowSeats.sort(Comparator.comparingInt(s -> s.num));
            List<Seat> run = new ArrayList<>();
            for (Seat s : rowSeats) {
                if (!s.booked) {
                    run.add(s);
                    if (run.size() == count) return Optional.of(run);
                } else {
                    run.clear();
                }
            }
        }
        return Optional.empty();
    }

    Booking book(String customer, int count) throws Exception {
        List<Seat> chosen = findBestAvailable(count)
                .orElseThrow(() -> new Exception("No " + count + " contiguous seats available"));
        for (Seat s : chosen) s.booked = true;
        Booking booking = new Booking("BK-" + bookingCounter++, customer, chosen);
        bookings.put(booking.id, booking);
        return booking;
    }

    void cancel(String bookingId) {
        Booking b = bookings.get(bookingId);
        if (b == null || b.cancelled) return;
        b.cancelled = true;
        for (Seat s : b.seats) s.booked = false;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleMovieBookingInterview show = new SimpleMovieBookingInterview();
        System.out.println("== Simple Movie Booking Interview Demo ==");

        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 4; num++) show.addSeat(new Seat("R" + row + "-" + num, row, num));
        }

        try {
            Booking b1 = show.book("Alice", 3);
            System.out.println("Alice booked: " + b1.seats);
            Booking b2 = show.book("Bob", 3);
            System.out.println("Bob booked: " + b2.seats);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }

        System.out.println("\nTrying to book 4 more seats (only 2 left):");
        try {
            show.book("Carol", 4);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nCancelling Alice's booking:");
        show.cancel("BK-1");

        System.out.println("Booking 3 seats again (should reuse Alice's freed seats):");
        try {
            Booking b3 = show.book("Dave", 3);
            System.out.println("Dave booked: " + b3.seats);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
