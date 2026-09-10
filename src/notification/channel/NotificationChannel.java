package notification.channel;

import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

// The single seam every delivery mechanism -- and every decorator around
// one -- implements. Throws on any failure (missing contact info, transient
// provider outage) so wrappers like RetryingChannel and the fallback chain
// can react uniformly without knowing which concrete channel they hold.
public interface NotificationChannel {
    NotificationChannelType getType();

    void send(User user, Notification notification) throws ChannelDeliveryException;

    class ChannelDeliveryException extends Exception {
        public ChannelDeliveryException(String message) { super(message); }
    }
}
