package notification.chain;

import notification.channel.NotificationChannel;
import notification.decorator.RetryingChannel;
import notification.factory.ChannelFactory;
import notification.model.NotificationChannelType;

import java.util.List;

// Assembles a per-user chain: for each preferred type, ask the factory for
// the channel, wrap it in a RetryingChannel (decorator), and link it to the
// previous handler. This is where the three patterns meet -- Factory
// supplies the links, Decorator dresses them, Chain of Responsibility
// strings them together -- and none of the three knows about the others.
public final class ChannelChain {
    private ChannelChain() {}

    public static ChannelHandler build(List<NotificationChannelType> preferenceOrder, ChannelFactory factory, int maxAttemptsPerChannel) {
        if (preferenceOrder.isEmpty()) throw new IllegalArgumentException("User has no notification channels configured");

        ChannelHandler head = null;
        ChannelHandler tail = null;
        for (NotificationChannelType type : preferenceOrder) {
            NotificationChannel channel = new RetryingChannel(factory.create(type), maxAttemptsPerChannel);
            ChannelHandler handler = new ChannelHandler(channel);
            if (head == null) {
                head = tail = handler;
            } else {
                tail = tail.linkNext(handler);
            }
        }
        return head;
    }
}
