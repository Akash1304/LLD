package airline.interview;

import java.util.*;

// Compact, single-file interview-friendly airline booking demo.
// Supports: seat assignment by class, demand-based pricing that rises as a
// class fills up, and cancellation.
public class SimpleAirlineInterview {

    static class Seat {
        final String id;
        final String seatClass;
        boolean booked = false;
        Seat(String id, String seatClass) { this.id = id; this.seatClass = seatClass; }
    }

    static class Booking {
        final String id;
        final String passenger;
        final Seat seat;
        final double price;
        boolean cancelled = false;
        Booking(String id, String passenger, Seat seat, double price) {
            this.id = id; this.passenger = passenger; this.seat = seat; this.price = price;
        }
    }

    final List<Seat> seats = new ArrayList<>();
    final Map<String, Booking> bookings = new HashMap<>();
    final double basePrice = 200.0;
    int counter = 1;

    void addSeat(Seat s) { seats.add(s); }

    long bookedInClass(String seatClass) {
        return seats.stream().filter(s -> s.seatClass.equals(seatClass) && s.booked).count();
    }

    long totalInClass(String seatClass) {
        return seats.stream().filter(s -> s.seatClass.equals(seatClass)).count();
    }

    double priceFor(Seat seat) {
        double occupancy = (double) bookedInClass(seat.seatClass) / totalInClass(seat.seatClass);
        return basePrice * (1 + occupancy * 0.5);
    }

    Booking book(String passenger, String seatClass) throws Exception {
        Seat seat = seats.stream().filter(s -> s.seatClass.equals(seatClass) && !s.booked).findFirst()
                .orElseThrow(() -> new Exception("No available " + seatClass + " seat"));
        double price = priceFor(seat);
        seat.booked = true;
        Booking booking = new Booking("BK-" + counter++, passenger, seat, price);
        bookings.put(booking.id, booking);
        return booking;
    }

    void cancel(String bookingId) {
        Booking b = bookings.get(bookingId);
        if (b == null || b.cancelled) return;
        b.cancelled = true;
        b.seat.booked = false;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleAirlineInterview airline = new SimpleAirlineInterview();
        System.out.println("== Simple Airline Interview Demo ==");

        for (int i = 1; i <= 4; i++) airline.addSeat(new Seat("ECO-" + i, "ECONOMY"));

        try {
            Booking b1 = airline.book("Alice", "ECONOMY");
            System.out.printf("Alice booked %s for $%.2f%n", b1.seat.id, b1.price);

            Booking b2 = airline.book("Bob", "ECONOMY");
            System.out.printf("Bob booked %s for $%.2f (price rose as the cabin fills)%n", b2.seat.id, b2.price);

            airline.book("Carol", "ECONOMY");
            airline.book("Dave", "ECONOMY");

            System.out.println("\nTrying a 5th economy booking (all seats taken):");
            try {
                airline.book("Eve", "ECONOMY");
            } catch (Exception e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nCancelling Alice's booking frees a seat:");
            airline.cancel(b1.id);
            Booking b5 = airline.book("Frank", "ECONOMY");
            System.out.printf("Frank booked %s%n", b5.seat.id);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
