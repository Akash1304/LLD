package calendar.interview;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Compact, single-file interview-friendly calendar demo.
// Supports: create events, detect overlap, show available slots, and shift to next slot.
public class SimpleCalendarInterview {
    static class SimpleEvent {
        final String id;
        final String title;
        final Instant start;
        final Instant end;
        SimpleEvent(String id, String title, Instant start, Instant end) {
            this.id = id; this.title = title; this.start = start; this.end = end;
        }
        boolean overlaps(SimpleEvent other) {
            return this.start.isBefore(other.end) && other.start.isBefore(this.end);
        }
        @Override public String toString() { return "Event{"+id+","+title+","+start+"->"+end+"}"; }
    }

    // in-memory store for one calendar
    final List<SimpleEvent> events = new ArrayList<>();
    int idCounter = 1;

    public SimpleEvent addEvent(Instant start, Instant end, String title) throws Exception {
        SimpleEvent candidate = new SimpleEvent(String.valueOf(idCounter++), title, start, end);
        List<SimpleEvent> conflicts = findConflicts(candidate);
        if (!conflicts.isEmpty()) throw new Exception("Conflicts: " + conflicts);
        events.add(candidate);
        events.sort(Comparator.comparing(e -> e.start));
        return candidate;
    }

    public List<SimpleEvent> findConflicts(SimpleEvent candidate) {
        List<SimpleEvent> conflicts = new ArrayList<>();
        for (SimpleEvent e : events) if (e.overlaps(candidate)) conflicts.add(e);
        return conflicts;
    }

    // find available timeslots within [from,to] that can fit duration
    public List<TimeSlot> findAvailableSlots(Instant from, Instant to, Duration duration) {
        List<TimeSlot> busy = new ArrayList<>();
        for (SimpleEvent e : events) {
            if (e.end.isBefore(from) || e.start.isAfter(to)) continue;
            Instant s = e.start.isBefore(from) ? from : e.start;
            Instant eend = e.end.isAfter(to) ? to : e.end;
            busy.add(new TimeSlot(s,eend));
        }
        busy.sort(Comparator.comparing(ts -> ts.start));
        // merge
        List<TimeSlot> merged = new ArrayList<>();
        for (TimeSlot ts : busy) {
            if (merged.isEmpty()) merged.add(ts);
            else {
                TimeSlot last = merged.get(merged.size()-1);
                if (!ts.start.isAfter(last.end)) {
                    // merge
                    Instant newEnd = ts.end.isAfter(last.end) ? ts.end : last.end;
                    merged.set(merged.size()-1, new TimeSlot(last.start, newEnd));
                } else merged.add(ts);
            }
        }
        List<TimeSlot> avail = new ArrayList<>();
        Instant cursor = from;
        for (TimeSlot b : merged) {
            if (cursor.isBefore(b.start)) {
                Duration gap = Duration.between(cursor, b.start);
                if (!gap.minus(duration).isNegative()) avail.add(new TimeSlot(cursor, b.start));
            }
            if (cursor.isBefore(b.end)) cursor = b.end;
        }
        if (cursor.isBefore(to)) {
            Duration gap = Duration.between(cursor, to);
            if (!gap.minus(duration).isNegative()) avail.add(new TimeSlot(cursor, to));
        }
        return avail;
    }

    static class TimeSlot {
        final Instant start; final Instant end;
        TimeSlot(Instant s, Instant e) { start=s; end=e; }
        @Override public String toString() { return "TimeSlot{"+start+"->"+end+"}"; }
    }

    // simple demo to run in ~5-10 minutes in interview
    public static void runDemo() {
        SimpleCalendarInterview cal = new SimpleCalendarInterview();
        try {
            System.out.println("== Simple Calendar Interview Demo ==");
            Instant dayStart = Instant.parse("2025-12-20T00:00:00Z");
            Instant dayEnd = Instant.parse("2025-12-20T23:59:59Z");

            System.out.println("Creating Event A 09:00-10:00");
            cal.addEvent(Instant.parse("2025-12-20T09:00:00Z"), Instant.parse("2025-12-20T10:00:00Z"), "A");

            System.out.println("Trying to create Event B 09:30-10:30 (should conflict)");
            try {
                cal.addEvent(Instant.parse("2025-12-20T09:30:00Z"), Instant.parse("2025-12-20T10:30:00Z"), "B");
            } catch (Exception ex) {
                System.out.println("Conflict detected: " + ex.getMessage());
                Duration dur = Duration.between(Instant.parse("2025-12-20T09:30:00Z"), Instant.parse("2025-12-20T10:30:00Z"));
                System.out.println("Available slots today for duration " + dur + ":");
                List<TimeSlot> slots = cal.findAvailableSlots(dayStart, dayEnd, dur);
                slots.forEach(System.out::println);
                if (!slots.isEmpty()) {
                    TimeSlot s = slots.get(0);
                    Instant newStart = s.start;
                    Instant newEnd = newStart.plus(dur);
                    System.out.println("Scheduling B in first available slot: " + newStart + "->" + newEnd);
                    cal.events.add(new SimpleEvent(String.valueOf(cal.idCounter++), "B", newStart, newEnd));
                }
            }

            System.out.println("Final schedule for the day:");
            cal.events.sort(Comparator.comparing(e -> e.start));
            cal.events.forEach(System.out::println);
            System.out.println("== Demo complete ==");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        runDemo();
    }
}

