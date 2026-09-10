package calendar.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Event {
    private final String id;
    private final String calendarId;
    private final String title;
    private final Instant start;
    private final Instant end;
    private final Optional<RecurrenceRule> recurrence;
    private final List<Attendee> attendees;
    private final String description;

    private Event(Builder b) {
        this.id = b.id;
        this.calendarId = b.calendarId;
        this.title = b.title;
        this.start = b.start;
        this.end = b.end;
        this.recurrence = Optional.ofNullable(b.recurrence);
        this.attendees = new ArrayList<>(b.attendees);
        this.description = b.description;
    }

    public static Builder builder() { return new Builder(); }

    // Copy-constructor-shaped builder: start from an existing event's
    // values, override a few, build a new immutable instance.
    public Builder toBuilder() {
        return new Builder()
                .id(id).calendarId(calendarId).title(title).start(start).end(end)
                .recurrence(recurrence.orElse(null)).attendees(attendees).description(description);
    }

    // Builder: Event has 8 fields, 3 of them optional (id is assigned by
    // the service, recurrence and description are often absent). An 8-arg
    // positional constructor is unreadable at the call site -- two Instants
    // and an Optional in a row are easy to transpose silently. The builder
    // names every field, supplies defaults for the optional ones, and
    // validates the invariant (start < end) once in build(), so an Event
    // can never exist in an invalid state.
    public static class Builder {
        private String id;
        private String calendarId;
        private String title;
        private Instant start;
        private Instant end;
        private RecurrenceRule recurrence;
        private List<Attendee> attendees = new ArrayList<>();
        private String description = "";

        public Builder id(String id) { this.id = id; return this; }
        public Builder calendarId(String calendarId) { this.calendarId = calendarId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder start(Instant start) { this.start = start; return this; }
        public Builder end(Instant end) { this.end = end; return this; }
        public Builder recurrence(RecurrenceRule recurrence) { this.recurrence = recurrence; return this; }
        public Builder attendees(List<Attendee> attendees) { this.attendees = new ArrayList<>(attendees); return this; }
        public Builder attendee(Attendee attendee) { this.attendees.add(attendee); return this; }
        public Builder description(String description) { this.description = description; return this; }

        public Event build() {
            Objects.requireNonNull(calendarId, "calendarId is required");
            Objects.requireNonNull(title, "title is required");
            Objects.requireNonNull(start, "start is required");
            Objects.requireNonNull(end, "end is required");
            if (!start.isBefore(end)) throw new IllegalArgumentException("start must be before end");
            return new Event(this);
        }
    }

    public String getId() { return id; }
    public String getCalendarId() { return calendarId; }
    public String getTitle() { return title; }
    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
    public Optional<RecurrenceRule> getRecurrence() { return recurrence; }
    public List<Attendee> getAttendees() { return new ArrayList<>(attendees); }
    public String getDescription() { return description; }

    public boolean overlaps(Event other) {
        // half-open interval overlap: [start, end) overlaps if start < other.end && other.start < end
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public Event copyWithStartEnd(Instant newStart, Instant newEnd) {
        return toBuilder().start(newStart).end(newEnd).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{" + "id='" + id + '\'' + ", title='" + title + '\'' + ", start=" + start + ", end=" + end + '}';
    }
}
