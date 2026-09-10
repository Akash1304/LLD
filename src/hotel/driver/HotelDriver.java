package hotel.driver;

import hotel.model.Guest;
import hotel.model.Reservation;
import hotel.model.Room;
import hotel.model.RoomType;
import hotel.service.InMemoryReservationService;
import hotel.service.InMemoryRoomInventoryService;
import hotel.service.ReservationService;
import hotel.service.RoomInventoryService;
import hotel.strategy.SeasonalPricingStrategy;

import java.time.LocalDate;

public class HotelDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        RoomInventoryService inventory = new InMemoryRoomInventoryService();
        ReservationService reservationService = new InMemoryReservationService(inventory);

        Room room101 = inventory.addRoom(new Room("101", RoomType.DOUBLE, 1));
        Room room102 = inventory.addRoom(new Room("102", RoomType.DOUBLE, 1));
        inventory.addRoom(new Room("201", RoomType.SUITE, 2));

        Guest alice = new Guest("G1", "Alice");
        Guest bob = new Guest("G2", "Bob");

        LocalDate dec28 = LocalDate.of(2025, 12, 28);
        LocalDate jan2 = LocalDate.of(2026, 1, 2);

        try {
            System.out.println("Booking a DOUBLE room over New Year's (holiday surcharge applies):");
            Reservation aliceRes = reservationService.bookRoom(alice, RoomType.DOUBLE, dec28, jan2, new SeasonalPricingStrategy());
            System.out.println(aliceRes);

            System.out.println("\nBooking another DOUBLE room for overlapping dates (should land on room 102):");
            Reservation bobRes = reservationService.bookRoom(bob, RoomType.DOUBLE, dec28, jan2, new SeasonalPricingStrategy());
            System.out.println(bobRes);

            System.out.println("\nTrying a third overlapping DOUBLE booking (no DOUBLE rooms left):");
            try {
                reservationService.bookRoom(new Guest("G3", "Carol"), RoomType.DOUBLE, dec28, jan2, new SeasonalPricingStrategy());
            } catch (ReservationService.NoRoomAvailableException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nAlice checks in, then checks out:");
            reservationService.checkIn(aliceRes.getId());
            System.out.println("After check-in: " + reservationService.getReservationsForGuest("G1"));
            reservationService.checkOut(aliceRes.getId());
            System.out.println("After check-out: " + reservationService.getReservationsForGuest("G1"));

            System.out.println("\nNow room 101 is free again -- booking a non-overlapping stay in January should succeed:");
            Reservation janRes = reservationService.bookRoom(new Guest("G4", "Dave"), RoomType.DOUBLE, jan2, jan2.plusDays(3), new SeasonalPricingStrategy());
            System.out.println(janRes);
        } catch (ReservationService.NoRoomAvailableException | ReservationService.InvalidReservationStateException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
