package airline.driver;

import airline.model.Booking;
import airline.model.Flight;
import airline.model.Passenger;
import airline.model.Seat;
import airline.model.SeatClass;
import airline.service.BookingService;
import airline.service.FlightService;
import airline.service.InMemoryBookingService;
import airline.service.InMemoryFlightService;
import airline.strategy.DemandBasedPricingStrategy;
import airline.strategy.FirstAvailableSeatAssignmentStrategy;
import airline.strategy.WindowSeatPreferredStrategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AirlineDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        FlightService flightService = new InMemoryFlightService();
        BookingService bookingService = new InMemoryBookingService(flightService);

        List<Seat> seats = new ArrayList<>();
        char[] economyCols = {'A', 'B', 'C', 'D', 'E', 'F'};
        for (int row = 10; row <= 11; row++) {
            for (char col : economyCols) seats.add(new Seat("R" + row + col, row, col, SeatClass.ECONOMY));
        }
        seats.add(new Seat("R1A", 1, 'A', SeatClass.BUSINESS));
        seats.add(new Seat("R1F", 1, 'F', SeatClass.BUSINESS));

        Flight flight = flightService.addFlight(Flight.builder()
                .id("FL1").flightNumber("AA100")
                .origin("JFK").destination("SFO")
                .departureTime(LocalDateTime.of(2026, 3, 15, 9, 0))
                .seats(seats).basePrice(200.0)
                .build());

        System.out.println("Flights JFK -> SFO: " + flightService.findFlights("JFK", "SFO"));

        try {
            System.out.println("\nBooking a window economy seat for Alice:");
            Booking aliceBooking = bookingService.bookSeat(flight.getId(), new Passenger("P1", "Alice"),
                    SeatClass.ECONOMY, new WindowSeatPreferredStrategy(), new DemandBasedPricingStrategy());
            System.out.println(aliceBooking);

            System.out.println("\nBooking 10 more economy seats to drive up demand-based pricing:");
            for (int i = 0; i < 10; i++) {
                bookingService.bookSeat(flight.getId(), new Passenger("P" + (i + 2), "Filler" + i),
                        SeatClass.ECONOMY, new FirstAvailableSeatAssignmentStrategy(), new DemandBasedPricingStrategy());
            }
            System.out.println("\nBooking one more economy seat -- price should be noticeably higher now that the cabin is fuller:");
            Booking laterBooking = bookingService.bookSeat(flight.getId(), new Passenger("P99", "Zara"),
                    SeatClass.ECONOMY, new FirstAvailableSeatAssignmentStrategy(), new DemandBasedPricingStrategy());
            System.out.println(laterBooking);

            System.out.println("\nBooking a business seat for Bob:");
            Booking bobBooking = bookingService.bookSeat(flight.getId(), new Passenger("P100", "Bob"),
                    SeatClass.BUSINESS, new FirstAvailableSeatAssignmentStrategy(), new DemandBasedPricingStrategy());
            System.out.println(bobBooking);

            System.out.println("\nCancelling Alice's booking:");
            bookingService.cancelBooking(aliceBooking.getId()).ifPresent(b -> System.out.println("Cancelled: " + b));

            System.out.println("\nAlice's bookings after cancellation: " + bookingService.getBookingsForPassenger("P1"));
        } catch (BookingService.NoSeatAvailableException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
