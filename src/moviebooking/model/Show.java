package moviebooking.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final double basePrice;
    private final Set<String> bookedSeatIds = new HashSet<>();

    public Show(String id, Movie movie, Screen screen, LocalDateTime startTime, double basePrice) {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.basePrice = basePrice;
    }

    public String getId() { return id; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public LocalDateTime getStartTime() { return startTime; }
    public double getBasePrice() { return basePrice; }

    public synchronized boolean isBooked(String seatId) { return bookedSeatIds.contains(seatId); }

    public synchronized boolean reserveSeats(Iterable<Seat> seats) {
        for (Seat s : seats) if (bookedSeatIds.contains(s.getId())) return false;
        for (Seat s : seats) bookedSeatIds.add(s.getId());
        return true;
    }

    public synchronized void releaseSeats(Iterable<Seat> seats) {
        for (Seat s : seats) bookedSeatIds.remove(s.getId());
    }

    @Override
    public String toString() { return movie.getTitle() + " @ " + startTime + " (" + screen.getName() + ")"; }
}
