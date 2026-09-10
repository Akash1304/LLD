package calendar.strategy;

import calendar.model.Event;
import calendar.model.TimeSlot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ConflictResolutionStrategy {
    // Find available TimeSlots within [from,to] for duration that can accommodate the event
    List<TimeSlot> findAvailableSlots(String calendarId, Instant from, Instant to, Duration duration);

    // Try to create event with strategy (e.g., shift to next available slot or reject)
    Event apply(Event desiredEvent) throws Exception;
}

