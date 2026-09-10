package hotel.model;

import java.time.LocalDate;
import java.util.Objects;

public class Reservation {
    private final String id;
    private final Guest guest;
    private final Room room;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final double totalPrice;
    private ReservationStatus status;

    private Reservation(Builder b) {
        this.id = b.id;
        this.guest = b.guest;
        this.room = b.room;
        this.checkInDate = b.checkInDate;
        this.checkOutDate = b.checkOutDate;
        this.totalPrice = b.totalPrice;
        this.status = ReservationStatus.BOOKED;
    }

    public static Builder builder() { return new Builder(); }

    // Builder: two adjacent LocalDate args (checkIn, checkOut) are the
    // classic transposition bug; naming them and validating checkIn <
    // checkOut in build() means an invalid stay can never be constructed.
    public static class Builder {
        private String id;
        private Guest guest;
        private Room room;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private double totalPrice;

        public Builder id(String id) { this.id = id; return this; }
        public Builder guest(Guest guest) { this.guest = guest; return this; }
        public Builder room(Room room) { this.room = room; return this; }
        public Builder checkIn(LocalDate checkInDate) { this.checkInDate = checkInDate; return this; }
        public Builder checkOut(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; return this; }
        public Builder totalPrice(double totalPrice) { this.totalPrice = totalPrice; return this; }

        public Reservation build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(guest, "guest is required");
            Objects.requireNonNull(room, "room is required");
            Objects.requireNonNull(checkInDate, "checkIn is required");
            Objects.requireNonNull(checkOutDate, "checkOut is required");
            if (!checkInDate.isBefore(checkOutDate)) throw new IllegalArgumentException("checkIn must be before checkOut");
            return new Reservation(this);
        }
    }

    public String getId() { return id; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public double getTotalPrice() { return totalPrice; }
    public synchronized ReservationStatus getStatus() { return status; }

    // Compare-and-set for a plain (non-atomic-typed) status field: the
    // check against `expected` and the write to `next` happen under one
    // monitor, so two concurrent requests racing to transition the same
    // reservation (e.g. a double-submitted check-in click, or a check-in
    // racing a cancellation) can't both succeed -- only one observes a
    // matching `expected` and wins.
    public synchronized boolean tryTransition(ReservationStatus expected, ReservationStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }

    // half-open date overlap: [checkIn, checkOut) so a checkout on the same
    // day as another reservation's check-in does not conflict
    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkInDate.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOutDate);
    }

    @Override
    public String toString() {
        return "Reservation{" + id + ", " + guest.getName() + ", " + room.getId()
                + ", " + checkInDate + "->" + checkOutDate + ", $" + totalPrice + ", " + status + '}';
    }
}
