package moviebooking.driver;

import moviebooking.model.Booking;
import moviebooking.model.Movie;
import moviebooking.model.Screen;
import moviebooking.model.Seat;
import moviebooking.model.SeatType;
import moviebooking.model.Show;
import moviebooking.service.BookingService;
import moviebooking.service.CatalogService;
import moviebooking.service.InMemoryBookingService;
import moviebooking.service.InMemoryCatalogService;
import moviebooking.strategy.BestAvailableSeatStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovieBookingDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        CatalogService catalogService = new InMemoryCatalogService();
        BookingService bookingService = new InMemoryBookingService(catalogService);

        Movie inception = catalogService.addMovie(new Movie("M1", "Inception", 148));

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 5; num++) {
                SeatType type = row == 1 ? SeatType.PREMIUM : SeatType.REGULAR;
                seats.add(new Seat("R" + row + "-" + num, row, num, type));
            }
        }
        Screen screen1 = catalogService.addScreen(new Screen("SCR1", "Screen 1", seats));

        Show show = catalogService.addShow(new Show("SHOW1", inception, screen1, LocalDateTime.of(2026, 1, 10, 19, 0), 12.0));

        System.out.println("Shows for 'Inception': " + catalogService.findShowsForMovie("Inception"));

        try {
            System.out.println("\nBooking 3 seats together (best-available strategy):");
            Booking booking1 = bookingService.bookSeats(show.getId(), "cust-alice", 3, new BestAvailableSeatStrategy());
            System.out.println(booking1);

            System.out.println("\nBooking 3 more seats (should land in row 2, since row 1 has 2 left):");
            Booking booking2 = bookingService.bookSeats(show.getId(), "cust-bob", 3, new BestAvailableSeatStrategy());
            System.out.println(booking2);
        } catch (BookingService.NotEnoughSeatsException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }

        System.out.println("\nTrying to book 10 seats (not enough left):");
        try {
            bookingService.bookSeats(show.getId(), "cust-carol", 10, new BestAvailableSeatStrategy());
        } catch (BookingService.NotEnoughSeatsException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nCancelling Alice's booking to free her seats:");
        List<Booking> aliceBookings = bookingService.getBookingsForCustomer("cust-alice");
        bookingService.cancelBooking(aliceBookings.get(0).getId())
                .ifPresent(b -> System.out.println("Cancelled: " + b));

        System.out.println("\nNow booking 3 seats again (should succeed, reusing Alice's freed seats):");
        try {
            Booking booking3 = bookingService.bookSeats(show.getId(), "cust-dave", 3, new BestAvailableSeatStrategy());
            System.out.println(booking3);
        } catch (BookingService.NotEnoughSeatsException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }
}
