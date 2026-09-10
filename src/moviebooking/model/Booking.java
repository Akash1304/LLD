package moviebooking.model;

import java.util.Collections;
import java.util.List;

public class Booking {
    private final String id;
    private final Show show;
    private final String customerId;
    private final List<Seat> seats;
    private final double totalPrice;
    private BookingStatus status;

    public Booking(String id, Show show, String customerId, List<Seat> seats, double totalPrice) {
        this.id = id;
        this.show = show;
        this.customerId = customerId;
        this.seats = seats;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getId() { return id; }
    public Show getShow() { return show; }
    public String getCustomerId() { return customerId; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }
    public double getTotalPrice() { return totalPrice; }
    public synchronized BookingStatus getStatus() { return status; }
    public synchronized void setStatus(BookingStatus status) { this.status = status; }

    // Atomic "cancel if still confirmed" -- without this, two concurrent
    // cancellation requests for the same booking (e.g. a double-submitted
    // click) could both pass a separate getStatus()-then-setStatus() check
    // and both call Show.releaseSeats(), double-releasing the same seats
    // back into the available pool.
    public synchronized boolean tryCancel() {
        if (status != BookingStatus.CONFIRMED) return false;
        status = BookingStatus.CANCELLED;
        return true;
    }

    @Override
    public String toString() {
        return "Booking{" + id + ", " + customerId + ", " + show + ", seats=" + seats + ", $" + totalPrice + ", " + status + '}';
    }
}
