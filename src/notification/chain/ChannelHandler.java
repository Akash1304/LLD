package notification.chain;

import notification.channel.NotificationChannel;
import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

import java.util.Optional;

// Chain of Responsibility: each handler owns one channel, tries to deliver,
// and on failure hands the request to the next handler -- it never knows
// how long the chain is or what comes after it. The chain is assembled per
// user from their preference order (see ChannelChain.build), so "try push,
// then email, then SMS" is data, not code.
//
// Why a chain and not a for-loop over channels in the service: the loop
// version hard-wires "what to do when a channel fails" into the
// orchestrator. With handlers, that decision lives with each link -- one
// handler could, for instance, short-circuit the whole chain on a
// permanent error (user opted out) instead of falling through, without the
// service changing at all.
public class ChannelHandler {
    private final NotificationChannel channel;
    private ChannelHandler next;

    public ChannelHandler(NotificationChannel channel) {
        this.channel = channel;
    }

    public ChannelHandler linkNext(ChannelHandler next) {
        this.next = next;
        return next;
    }

    // returns the channel type that actually delivered, or empty if every
    // link in the chain (from here on) failed
    public Optional<NotificationChannelType> handle(User user, Notification notification) {
        try {
            channel.send(user, notification);
            return Optional.of(channel.getType());
        } catch (NotificationChannel.ChannelDeliveryException e) {
            System.out.println("  " + channel.getType() + " gave up (" + e.getMessage() + ")"
                    + (next != null ? ", falling back to " + next.channel.getType() : ", no channels left"));
            return next == null ? Optional.empty() : next.handle(user, notification);
        }
    }
}
