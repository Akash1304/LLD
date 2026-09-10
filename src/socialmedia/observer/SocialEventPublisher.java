package socialmedia.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// The Observer "subject". PostService/UserService publish here and have no
// idea who is listening -- notifications, an analytics counter, a feed
// cache invalidator can all subscribe without those services changing.
//
// Why Observer here and not a direct NotificationService call inside
// PostService: the domain says "notifications" today, but "update the
// like counter cache" and "emit an analytics event" are the next two asks,
// and each would otherwise mean another hard dependency bolted onto
// PostService. Publishing decouples the *fact* (a like happened) from
// every *reaction* to it.
public class SocialEventPublisher {
    // CopyOnWriteArrayList: subscriptions are rare, publishes are constant,
    // and iterating a snapshot means a listener subscribing mid-publish
    // can't throw ConcurrentModificationException.
    private final List<SocialEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(SocialEventListener listener) { listeners.add(listener); }
    public void unsubscribe(SocialEventListener listener) { listeners.remove(listener); }

    public void publish(SocialEvent event) {
        for (SocialEventListener listener : listeners) listener.onEvent(event);
    }
}
