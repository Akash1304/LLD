package airline.model;

public class Booking {
    private final String id;
    private final Flight flight;
    private final Passenger passenger;
    private final Seat seat;
    private final double price;
    private BookingStatus status;

    public Booking(String id, Flight flight, Passenger passenger, Seat seat, double price) {
        this.id = id;
        this.flight = flight;
        this.passenger = passenger;
        this.seat = seat;
        this.price = price;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getId() { return id; }
    public Flight getFlight() { return flight; }
    public Passenger getPassenger() { return passenger; }
    public Seat getSeat() { return seat; }
    public double getPrice() { return price; }
    public synchronized BookingStatus getStatus() { return status; }

    // Atomic "cancel if still confirmed" -- prevents two concurrent
    // cancellation requests for the same booking from both succeeding and
    // both calling Flight.releaseSeat(), which would double-release the
    // seat back into the available pool.
    public synchronized boolean tryCancel() {
        if (status != BookingStatus.CONFIRMED) return false;
        status = BookingStatus.CANCELLED;
        return true;
    }

    @Override
    public String toString() {
        return "Booking{" + id + ", " + passenger.getName() + ", " + flight.getFlightNumber()
                + ", seat=" + seat + ", $" + String.format("%.2f", price) + ", " + status + '}';
    }
}
