package notification.factory;

import notification.channel.EmailChannel;
import notification.channel.NotificationChannel;
import notification.channel.PushChannel;
import notification.channel.SmsChannel;
import notification.model.NotificationChannelType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Factory: the ONE place that knows how to turn a NotificationChannelType
// into a concrete channel. Callers (the service, the chain builder) work
// purely in terms of the enum + the NotificationChannel interface and never
// name a concrete class -- adding a SlackChannel means one new case here,
// nothing else changes.
//
// Instances are created once and cached per type (computeIfAbsent is
// atomic on ConcurrentHashMap), because real channels hold expensive,
// shareable state -- an SMTP/APNs connection pool -- and PushChannel here
// keeps its per-notification attempt counter, which must survive across
// the retries of a single send.
public class ChannelFactory {
    private final int pushFailuresBeforeSuccess;
    private final Map<NotificationChannelType, NotificationChannel> cache = new ConcurrentHashMap<>();

    public ChannelFactory(int pushFailuresBeforeSuccess) {
        this.pushFailuresBeforeSuccess = pushFailuresBeforeSuccess;
    }

    public NotificationChannel create(NotificationChannelType type) {
        return cache.computeIfAbsent(type, t -> {
            switch (t) {
                case EMAIL: return new EmailChannel();
                case SMS:   return new SmsChannel();
                case PUSH:  return new PushChannel(pushFailuresBeforeSuccess);
                default:    throw new IllegalArgumentException("No channel registered for " + t);
            }
        });
    }
}
