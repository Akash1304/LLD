package calendar.service;

import calendar.model.Event;
import calendar.model.TimeSlot;
import calendar.observer.EventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventService {
    void addListener(EventListener listener);
    Event createEvent(Event event) throws ConflictException;
    Event createEvent(Event event, boolean force) throws ConflictException;
    Optional<Event> updateEvent(Event event) throws ConflictException;
    boolean deleteEvent(String id);
    List<Event> listEvents(String calendarId, Instant from, Instant to);
    List<Event> expandRecurring(Event event, Instant from, Instant to);
    List<Event> findConflicts(String calendarId, Event candidate);
    List<TimeSlot> findAvailableSlots(String calendarId, Instant from, Instant to, Duration duration);

    class ConflictException extends Exception {
        private final List<Event> conflicts;
        public ConflictException(List<Event> conflicts) {
            super("Conflicts detected: " + conflicts);
            this.conflicts = conflicts;
        }
        public List<Event> getConflicts() { return conflicts; }
    }
}
