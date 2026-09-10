package notification.driver;

import notification.factory.ChannelFactory;
import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;
import notification.service.InMemoryNotificationService;
import notification.service.InMemoryUserDirectory;
import notification.service.NotificationService;
import notification.service.UserDirectory;

import java.util.Arrays;

public class NotificationDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        UserDirectory userDirectory = new InMemoryUserDirectory();

        // Alice prefers push, falling back to email; the push provider will
        // fail her first 2 attempts before succeeding on the 3rd
        userDirectory.addUser(new User("alice", "Alice", "alice@example.com", "555-1000",
                Arrays.asList(NotificationChannelType.PUSH, NotificationChannelType.EMAIL)));

        // Bob has no push device or phone registered -- only email works
        userDirectory.addUser(new User("bob", "Bob", "bob@example.com", null,
                Arrays.asList(NotificationChannelType.PUSH, NotificationChannelType.SMS, NotificationChannelType.EMAIL)));

        // the factory is the only place that knows concrete channel classes;
        // the push provider is configured to fail twice per notification
        ChannelFactory channelFactory = new ChannelFactory(2);
        NotificationService notificationService = new InMemoryNotificationService(userDirectory, channelFactory);

        System.out.println("Sending to Alice (push fails twice, 3 attempts allowed per channel, succeeds on 3rd):");
        Notification aliceNotif = notificationService.send("alice", "Order Shipped", "Your order is on the way!", 3);
        System.out.println(aliceNotif);

        System.out.println("\nSending to Bob (push has no device, SMS has no phone, chain falls back to email):");
        Notification bobNotif = notificationService.send("bob", "Password Reset", "Click here to reset your password", 1);
        System.out.println(bobNotif);

        System.out.println("\nSending to Alice again with only 1 attempt per channel (push can't recover in time, falls back to email):");
        Notification aliceNotif2 = notificationService.send("alice", "Reminder", "Don't forget your appointment", 1);
        System.out.println(aliceNotif2);
    }
}
