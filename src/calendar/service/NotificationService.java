package calendar.service;

import calendar.observer.EventListener;

// Notifications are an OBSERVER of the event service, not something the
// caller invokes by hand. Before this, CalendarDriver had to remember to
// call notifyInvite() after every createEvent() -- forget once and invites
// silently don't go out. Now EventService publishes and this reacts.
public interface NotificationService extends EventListener {
}
