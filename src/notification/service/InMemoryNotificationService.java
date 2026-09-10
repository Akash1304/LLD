package notification.service;

import notification.chain.ChannelChain;
import notification.chain.ChannelHandler;
import notification.factory.ChannelFactory;
import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.NotificationStatus;
import notification.model.User;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// The orchestrator is now almost empty on purpose: it looks up the user,
// asks ChannelChain to assemble the right handlers for that user, and lets
// the chain run. Retry lives in the decorator, fallback lives in the chain,
// channel construction lives in the factory -- none of it here.
public class InMemoryNotificationService implements NotificationService {
    private final UserDirectory userDirectory;
    private final ChannelFactory channelFactory;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryNotificationService(UserDirectory userDirectory, ChannelFactory channelFactory) {
        this.userDirectory = userDirectory;
        this.channelFactory = channelFactory;
    }

    @Override
    public Notification send(String userId, String title, String message, int maxAttemptsPerChannel) {
        User user = userDirectory.getUser(userId).orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));
        Notification notification = new Notification("NOTIF-" + idCounter.getAndIncrement(), userId, title, message);

        ChannelHandler chain = ChannelChain.build(user.getChannelPreferenceOrder(), channelFactory, maxAttemptsPerChannel);
        Optional<NotificationChannelType> deliveredVia = chain.handle(user, notification);

        if (deliveredVia.isPresent()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setDeliveredVia(deliveredVia.get());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
        }
        return notification;
    }
}
