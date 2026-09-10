package calendar.service;

import calendar.model.Attendee;
import calendar.model.Event;

public class InMemoryNotificationService implements NotificationService {
    @Override
    public void onEventCreated(Event event) {
        for (Attendee attendee : event.getAttendees()) {
            System.out.println("[Notification] Invite sent to " + attendee.getUser().getEmail() + " for event " + event.getTitle());
        }
    }

    @Override
    public void onEventUpdated(Event event) {
        for (Attendee attendee : event.getAttendees()) {
            System.out.println("[Notification] Update sent to " + attendee.getUser().getEmail() + " for event " + event.getTitle());
        }
    }
}
