package notification.channel;

import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

public class SmsChannel implements NotificationChannel {
    @Override
    public NotificationChannelType getType() { return NotificationChannelType.SMS; }

    @Override
    public void send(User user, Notification notification) throws ChannelDeliveryException {
        if (user.getPhone() == null) {
            throw new ChannelDeliveryException("User " + user.getId() + " has no phone on file");
        }
        System.out.println("  [SMS to " + user.getPhone() + "] " + notification.getTitle() + ": " + notification.getMessage());
    }
}
