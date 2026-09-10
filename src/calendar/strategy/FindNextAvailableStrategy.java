package calendar.strategy;

import calendar.model.Event;
import calendar.model.TimeSlot;
import calendar.service.EventService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class FindNextAvailableStrategy implements ConflictResolutionStrategy {
    private final EventService eventService;
    private final Instant searchTo;

    public FindNextAvailableStrategy(EventService eventService, Instant searchTo) {
        this.eventService = eventService;
        this.searchTo = searchTo;
    }

    @Override
    public List<TimeSlot> findAvailableSlots(String calendarId, Instant from, Instant to, Duration duration) {
        return eventService.findAvailableSlots(calendarId, from, to, duration);
    }

    @Override
    public Event apply(Event desiredEvent) throws Exception {
        Duration dur = Duration.between(desiredEvent.getStart(), desiredEvent.getEnd());
        Instant from = desiredEvent.getStart();
        Instant to = (searchTo != null) ? searchTo : from.plus(Duration.ofDays(7));
        List<TimeSlot> slots = eventService.findAvailableSlots(desiredEvent.getCalendarId(), from, to, dur);
        if (slots.isEmpty()) throw new Exception("No available slot found for duration " + dur);
        TimeSlot s = slots.get(0);
        Instant newStart = s.getStart();
        Instant newEnd = newStart.plus(dur);
        return desiredEvent.copyWithStartEnd(newStart, newEnd);
    }
}

