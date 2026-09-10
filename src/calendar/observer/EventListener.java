package calendar.observer;

import calendar.model.Event;

// Observer contract for calendar lifecycle events. Default no-op methods
// so a listener that only cares about creation doesn't have to stub out
// the rest.
public interface EventListener {
    default void onEventCreated(Event event) {}
    default void onEventUpdated(Event event) {}
    default void onEventDeleted(Event event) {}
}
