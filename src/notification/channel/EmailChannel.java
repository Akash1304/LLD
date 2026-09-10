package notification.channel;

import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

public class EmailChannel implements NotificationChannel {
    @Override
    public NotificationChannelType getType() { return NotificationChannelType.EMAIL; }

    @Override
    public void send(User user, Notification notification) throws ChannelDeliveryException {
        if (user.getEmail() == null) {
            throw new ChannelDeliveryException("User " + user.getId() + " has no email on file");
        }
        System.out.println("  [EMAIL to " + user.getEmail() + "] " + notification.getTitle() + ": " + notification.getMessage());
    }
}
