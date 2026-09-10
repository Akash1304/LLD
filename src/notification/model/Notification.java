package notification.model;

import java.time.Instant;

public class Notification {
    private final String id;
    private final String userId;
    private final String title;
    private final String message;
    private final Instant createdAt;
    private NotificationStatus status;
    private NotificationChannelType deliveredVia;

    public Notification(String id, String userId, String title, String message) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = NotificationStatus.PENDING;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public NotificationChannelType getDeliveredVia() { return deliveredVia; }
    public void setDeliveredVia(NotificationChannelType deliveredVia) { this.deliveredVia = deliveredVia; }

    @Override
    public String toString() {
        return "Notification{" + id + ", " + userId + ", \"" + title + "\", " + status
                + (deliveredVia != null ? " via " + deliveredVia : "") + '}';
    }
}
