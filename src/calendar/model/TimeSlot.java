package calendar.model;

import java.time.Instant;
import java.util.Objects;

public class TimeSlot {
    private final Instant start;
    private final Instant end;

    public TimeSlot(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }

    @Override
    public String toString() {
        return "TimeSlot{" + "start=" + start + ", end=" + end + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(start, timeSlot.start) && Objects.equals(end, timeSlot.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}

