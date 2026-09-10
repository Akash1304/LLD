package notification.decorator;

import notification.channel.NotificationChannel;
import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

// Decorator: adds bounded retry to ANY NotificationChannel without that
// channel knowing about retries at all. Because it implements the same
// interface it wraps, it can sit anywhere a plain channel can -- including
// inside the fallback chain -- and can itself be wrapped again (e.g. by a
// future RateLimitedChannel or MetricsChannel).
//
// Why a decorator and not a "RetryPolicy" object handed to the service:
// retry is a property of a delivery attempt, not of the orchestration
// layer. Putting it here keeps the service ignorant of retries entirely
// and lets different channels carry different retry budgets (aggressive
// for push, none for email) by simply wrapping them differently.
public class RetryingChannel implements NotificationChannel {
    private final NotificationChannel delegate;
    private final int maxAttempts;

    public RetryingChannel(NotificationChannel delegate, int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public NotificationChannelType getType() { return delegate.getType(); }

    @Override
    public void send(User user, Notification notification) throws ChannelDeliveryException {
        ChannelDeliveryException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                delegate.send(user, notification);
                return;
            } catch (ChannelDeliveryException e) {
                last = e;
                System.out.println("  Attempt " + attempt + "/" + maxAttempts + " via " + getType() + " failed: " + e.getMessage());
            }
        }
        throw new ChannelDeliveryException(getType() + " exhausted " + maxAttempts + " attempt(s): " + last.getMessage());
    }
}
