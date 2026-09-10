package notification.channel;

import notification.model.Notification;
import notification.model.NotificationChannelType;
import notification.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Simulates a flaky push provider: the first `failuresBeforeSuccess`
// attempts for a given notification fail (e.g. transient network issues),
// then it succeeds -- deterministic, so the retry demo is reproducible
// rather than actually random.
public class PushChannel implements NotificationChannel {
    private final int failuresBeforeSuccess;
    // ConcurrentHashMap.merge is atomic per key: retries for DIFFERENT
    // notifications through this same shared channel instance never
    // corrupt each other's attempt counts.
    private final Map<String, Integer> attemptsSoFar = new ConcurrentHashMap<>();

    public PushChannel(int failuresBeforeSuccess) {
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Override
    public NotificationChannelType getType() { return NotificationChannelType.PUSH; }

    @Override
    public void send(User user, Notification notification) throws ChannelDeliveryException {
        int attempts = attemptsSoFar.merge(notification.getId(), 1, Integer::sum);
        if (attempts <= failuresBeforeSuccess) {
            throw new ChannelDeliveryException("Push provider transiently unavailable (attempt " + attempts + ")");
        }
        System.out.println("  [PUSH to " + user.getId() + "'s device] " + notification.getTitle() + ": " + notification.getMessage());
    }
}
