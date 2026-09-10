package calendar.service;

import calendar.model.Calendar;
import calendar.model.Event;
import calendar.model.RecurrenceRule;
import calendar.model.TimeSlot;
import calendar.observer.EventListener;
import calendar.util.IdGenerator;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryEventService implements EventService {
    private final Map<String, Event> events = new ConcurrentHashMap<>();
    private final CalendarService calendarService;
    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();
    // Only events within the SAME calendar can conflict with each other, so
    // the natural lock granularity for the check-then-act in
    // createEvent/updateEvent (check for conflicts, then commit) is "per
    // calendar" -- not one global lock across every calendar in the
    // system, and not per-event either (a new event has no lock of its own
    // to acquire yet). Lazily created, one monitor object per calendarId.
    private final Map<String, Object> calendarLocks = new ConcurrentHashMap<>();

    public InMemoryEventService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    private Object lockFor(String calendarId) {
        return calendarLocks.computeIfAbsent(calendarId, k -> new Object());
    }

    @Override
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public Event createEvent(Event event) throws ConflictException {
        return createEvent(event, false);
    }

    @Override
    public Event createEvent(Event event, boolean force) throws ConflictException {
        Event stored;
        synchronized (lockFor(event.getCalendarId())) {
            List<Event> conflicts = findConflicts(event.getCalendarId(), event);
            if (!conflicts.isEmpty() && !force) {
                throw new ConflictException(conflicts);
            }
            String id = event.getId() == null ? IdGenerator.nextId() : event.getId();
            stored = event.toBuilder().id(id).build();
            events.put(id, stored);
            // add to calendar
            calendarService.getCalendar(event.getCalendarId()).ifPresent(c -> c.addEvent(id));
        }
        // notify OUTSIDE the calendar lock: a slow listener (real email
        // send) must not hold up every other booking on this calendar
        for (EventListener l : listeners) l.onEventCreated(stored);
        return stored;
    }

    @Override
    public Optional<Event> updateEvent(Event event) throws ConflictException {
        synchronized (lockFor(event.getCalendarId())) {
            List<Event> conflicts = findConflicts(event.getCalendarId(), event);
            // allow itself
            conflicts = conflicts.stream().filter(e -> !e.getId().equals(event.getId())).collect(Collectors.toList());
            if (!conflicts.isEmpty()) throw new ConflictException(conflicts);
            if (!events.containsKey(event.getId())) return Optional.empty();
            events.put(event.getId(), event);
        }
        for (EventListener l : listeners) l.onEventUpdated(event);
        return Optional.of(event);
    }

    @Override
    public boolean deleteEvent(String id) {
        Event removed = events.remove(id);
        if (removed != null) {
            calendarService.getCalendar(removed.getCalendarId()).ifPresent(c -> c.removeEvent(id));
            return true;
        }
        return false;
    }

    @Override
    public List<Event> listEvents(String calendarId, Instant from, Instant to) {
        Calendar cal = calendarService.getCalendar(calendarId).orElse(null);
        if (cal == null) return Collections.emptyList();
        List<Event> result = new ArrayList<>();
        for (String eid : cal.getEventIds()) {
            Event e = events.get(eid);
            if (e == null) continue;
            if (e.getRecurrence().isPresent()) {
                result.addAll(expandRecurring(e, from, to));
            } else {
                if (!(e.getEnd().isBefore(from) || e.getStart().isAfter(to))) {
                    result.add(e);
                }
            }
        }
        result.sort(Comparator.comparing(Event::getStart));
        return result;
    }

    @Override
    public List<Event> expandRecurring(Event event, Instant from, Instant to) {
        List<Event> occurrences = new ArrayList<>();
        Optional<RecurrenceRule> rrOpt = event.getRecurrence();
        if (!rrOpt.isPresent()) return occurrences;
        RecurrenceRule rr = rrOpt.get();

        // operate in UTC
        ZonedDateTime curStart = ZonedDateTime.ofInstant(event.getStart(), ZoneOffset.UTC);
        ZonedDateTime curEnd = ZonedDateTime.ofInstant(event.getEnd(), ZoneOffset.UTC);

        int generated = 0;
        Integer countLimit = rr.getCount();
        LocalDateTime until = rr.getUntil();

        while (true) {
            Instant s = curStart.toInstant();
            Instant e = curEnd.toInstant();
            if (!e.isBefore(from) && !s.isAfter(to)) {
                occurrences.add(event.copyWithStartEnd(s, e));
            }
            generated++;
            if (countLimit != null && generated >= countLimit) break;
            if (until != null && curStart.toLocalDateTime().isAfter(until)) break;

            // advance
            switch (rr.getFrequency()) {
                case DAILY:
                    curStart = curStart.plusDays(rr.getInterval());
                    curEnd = curEnd.plusDays(rr.getInterval());
                    break;
                case WEEKLY:
                    // if byDay specified, step forward day-by-day within week
                    if (rr.getByDay() != null && !rr.getByDay().isEmpty()) {
                        // find next byDay after current start
                        DayOfWeek curDow = curStart.getDayOfWeek();
                        boolean found = false;
                        for (int i = 1; i <= 7; i++) {
                            ZonedDateTime candidate = curStart.plusDays(i);
                            if (rr.getByDay().contains(candidate.getDayOfWeek())) {
                                // shift by the delta
                                long delta = Duration.between(curStart.toLocalDate().atStartOfDay(), candidate.toLocalDate().atStartOfDay()).toDays();
                                curStart = curStart.plusDays(i);
                                curEnd = curEnd.plusDays(i);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            // jump by weeks*interval
                            curStart = curStart.plusWeeks(rr.getInterval());
                            curEnd = curEnd.plusWeeks(rr.getInterval());
                        }
                    } else {
                        curStart = curStart.plusWeeks(rr.getInterval());
                        curEnd = curEnd.plusWeeks(rr.getInterval());
                    }
                    break;
                case MONTHLY:
                    curStart = curStart.plusMonths(rr.getInterval());
                    curEnd = curEnd.plusMonths(rr.getInterval());
                    break;
            }

            // stop if we've gone past 'to' enough
            if (curStart.toInstant().isAfter(to)) break;
        }

        return occurrences.stream().sorted(Comparator.comparing(Event::getStart)).collect(Collectors.toList());
    }

    @Override
    public List<Event> findConflicts(String calendarId, Event candidate) {
        // gather events in candidate's date window
        Instant from = candidate.getStart();
        Instant to = candidate.getEnd();
        List<Event> conflicts = new ArrayList<>();
        List<Event> existing = listEvents(calendarId, from.minus(Duration.ofDays(365)), to.plus(Duration.ofDays(365)));
        for (Event e : existing) {
            if (e.getId().equals(candidate.getId())) continue; // skip same
            if (e.overlaps(candidate)) {
                conflicts.add(e);
            }
        }
        return conflicts;
    }

    // New implementation: compute available slots by merging busy intervals and finding gaps
    @Override
    public List<TimeSlot> findAvailableSlots(String calendarId, Instant from, Instant to, Duration duration) {
        List<Event> busy = listEvents(calendarId, from, to);
        List<TimeSlot> busySlots = busy.stream().map(e -> new TimeSlot(e.getStart(), e.getEnd())).sorted(Comparator.comparing(TimeSlot::getStart)).collect(Collectors.toList());

        List<TimeSlot> merged = new ArrayList<>();
        for (TimeSlot slot : busySlots) {
            if (merged.isEmpty()) {
                merged.add(slot);
            } else {
                TimeSlot last = merged.get(merged.size() - 1);
                if (!slot.getStart().isAfter(last.getEnd())) {
                    // overlap or contiguous -> merge
                    TimeSlot mergedSlot = new TimeSlot(last.getStart(), slot.getEnd().isAfter(last.getEnd()) ? slot.getEnd() : last.getEnd());
                    merged.set(merged.size() - 1, mergedSlot);
                } else {
                    merged.add(slot);
                }
            }
        }

        List<TimeSlot> available = new ArrayList<>();
        Instant cursor = from;
        for (TimeSlot busySlot : merged) {
            if (cursor.isBefore(busySlot.getStart())) {
                Duration gap = Duration.between(cursor, busySlot.getStart());
                if (!gap.minus(duration).isNegative()) { // gap >= duration
                    available.add(new TimeSlot(cursor, busySlot.getStart()));
                }
            }
            if (cursor.isBefore(busySlot.getEnd())) cursor = busySlot.getEnd();
        }
        // tail gap
        if (cursor.isBefore(to)) {
            Duration gap = Duration.between(cursor, to);
            if (!gap.minus(duration).isNegative()) available.add(new TimeSlot(cursor, to));
        }
        return available;
    }
}
