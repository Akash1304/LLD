package hotel.interview;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Compact, single-file interview-friendly hotel demo.
// Supports: booking a room for a date range with overlap checking,
// check-in/check-out, and cancellation.
public class SimpleHotelInterview {

    static class Room {
        final String id;
        final double nightlyRate;
        Room(String id, double nightlyRate) { this.id = id; this.nightlyRate = nightlyRate; }
    }

    enum Status { BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    static class Reservation {
        final String id;
        final Room room;
        final String guest;
        final LocalDate checkIn;
        final LocalDate checkOut;
        final double total;
        Status status = Status.BOOKED;
        Reservation(String id, Room room, String guest, LocalDate checkIn, LocalDate checkOut, double total) {
            this.id = id; this.room = room; this.guest = guest; this.checkIn = checkIn; this.checkOut = checkOut; this.total = total;
        }
        boolean overlaps(LocalDate otherIn, LocalDate otherOut) {
            return checkIn.isBefore(otherOut) && otherIn.isBefore(checkOut);
        }
    }

    final List<Room> rooms = new ArrayList<>();
    final List<Reservation> reservations = new ArrayList<>();
    int counter = 1;

    void addRoom(Room r) { rooms.add(r); }

    Reservation book(String guest, LocalDate checkIn, LocalDate checkOut) throws Exception {
        for (Room room : rooms) {
            boolean free = reservations.stream()
                    .filter(r -> r.room == room && r.status != Status.CANCELLED && r.status != Status.CHECKED_OUT)
                    .noneMatch(r -> r.overlaps(checkIn, checkOut));
            if (free) {
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                Reservation res = new Reservation("RES-" + counter++, room, guest, checkIn, checkOut, nights * room.nightlyRate);
                reservations.add(res);
                return res;
            }
        }
        throw new Exception("No room available for " + checkIn + " -> " + checkOut);
    }

    void checkIn(String id) { find(id).status = Status.CHECKED_IN; }
    void checkOut(String id) { find(id).status = Status.CHECKED_OUT; }

    Reservation find(String id) {
        return reservations.stream().filter(r -> r.id.equals(id)).findFirst().orElseThrow();
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleHotelInterview hotel = new SimpleHotelInterview();
        System.out.println("== Simple Hotel Interview Demo ==");

        hotel.addRoom(new Room("101", 120.0));
        hotel.addRoom(new Room("102", 120.0));

        LocalDate in = LocalDate.of(2026, 3, 1);
        LocalDate out = LocalDate.of(2026, 3, 4);

        try {
            Reservation aliceRes = hotel.book("Alice", in, out);
            System.out.println("Alice booked " + aliceRes.room.id + ", total=$" + aliceRes.total);
            Reservation bobRes = hotel.book("Bob", in, out);
            System.out.println("Bob booked " + bobRes.room.id + ", total=$" + bobRes.total);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }

        System.out.println("\nTrying a third overlapping booking (no rooms left):");
        try {
            hotel.book("Carol", in, out);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nAlice checks in and out; room 101 frees up:");
        hotel.checkIn("RES-1");
        hotel.checkOut("RES-1");

        System.out.println("Booking a non-overlapping stay for the same dates should now succeed on room 101:");
        try {
            Reservation daveRes = hotel.book("Dave", in, out);
            System.out.println("Dave booked " + daveRes.room.id);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
