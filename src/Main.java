import airline.interview.SimpleAirlineInterview;
import atm.interview.SimpleATMInterview;
import bookstore.interview.SimpleBookstoreInterview;
import calendar.interview.SimpleCalendarInterview;
import chatapp.interview.SimpleChatAppInterview;
import ecommerce.interview.SimpleEcommerceInterview;
import elevator.interview.SimpleElevatorInterview;
import fooddelivery.interview.SimpleFoodDeliveryInterview;
import filestorage.interview.SimpleFileStorageInterview;
import hotel.interview.SimpleHotelInterview;
import library.interview.SimpleLibraryInterview;
import moviebooking.interview.SimpleMovieBookingInterview;
import notification.interview.SimpleNotificationInterview;
import parkinglot.interview.SimpleParkingLotInterview;
import ridesharing.interview.SimpleRideSharingInterview;
import shoppingcart.interview.SimpleShoppingCartInterview;
import snakeladder.interview.SimpleSnakeLadderInterview;
import socialmedia.interview.SimpleSocialMediaInterview;

// Smoke-test entrypoint: runs every LLD's compact interview demo back to
// back. Prefer running each driver's own main directly when iterating on
// one LLD at a time - see README.md.
public class Main {
    public static void main(String[] args) {
        SimpleCalendarInterview.runDemo();
        System.out.println();
        SimpleParkingLotInterview.runDemo();
        System.out.println();
        SimpleSnakeLadderInterview.runDemo();
        System.out.println();
        SimpleBookstoreInterview.runDemo();
        System.out.println();
        SimpleLibraryInterview.runDemo();
        System.out.println();
        SimpleMovieBookingInterview.runDemo();
        System.out.println();
        SimpleElevatorInterview.runDemo();
        System.out.println();
        SimpleHotelInterview.runDemo();
        System.out.println();
        SimpleRideSharingInterview.runDemo();
        System.out.println();
        SimpleFileStorageInterview.runDemo();
        System.out.println();
        SimpleChatAppInterview.runDemo();
        System.out.println();
        SimpleSocialMediaInterview.runDemo();
        System.out.println();
        SimpleNotificationInterview.runDemo();
        System.out.println();
        SimpleAirlineInterview.runDemo();
        System.out.println();
        SimpleATMInterview.runDemo();
        System.out.println();
        SimpleEcommerceInterview.runDemo();
        System.out.println();
        SimpleFoodDeliveryInterview.runDemo();
        System.out.println();
        SimpleShoppingCartInterview.runDemo();
    }
}
