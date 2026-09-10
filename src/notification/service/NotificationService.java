package notification.service;

import notification.model.Notification;

public interface NotificationService {
    Notification send(String userId, String title, String message, int maxAttemptsPerChannel);
}
