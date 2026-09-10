package calendar.driver;

import calendar.model.*;
import calendar.model.Calendar;
import calendar.service.*;

import calendar.util.IdGenerator;
import calendar.strategy.FindNextAvailableStrategy;

import java.time.*;
import java.util.*;

public class CalendarDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        CalendarService calendarService = new InMemoryCalendarService();
        EventService eventService = new InMemoryEventService(calendarService);
        // Observer: notifications subscribe to the event service once;
        // nobody has to remember to call notify() after each create
        NotificationService notificationService = new InMemoryNotificationService();
        eventService.addListener(notificationService);

        // create users
        User alice = new User(IdGenerator.nextId(), "Alice", "alice@example.com");
        User bob = new User(IdGenerator.nextId(), "Bob", "bob@example.com");

        // create calendar
        Calendar cal = calendarService.createCalendar(alice, "Alice Work");
        System.out.println("Created calendar: " + cal);

        // create an event 2025-12-20T09:00Z to 10:00Z
        Instant e1Start = Instant.parse("2025-12-20T09:00:00Z");
        Instant e1End = Instant.parse("2025-12-20T10:00:00Z");
        Event e1 = Event.builder()
                .calendarId(cal.getId()).title("Morning Meeting")
                .start(e1Start).end(e1End)
                .attendee(new Attendee(bob, Attendee.ResponseStatus.INVITED))
                .description("Discuss Q4")
                .build();
        try {
            Event stored1 = eventService.createEvent(e1);
            System.out.println("Created event: " + stored1);
        } catch (EventService.ConflictException ex) {
            System.out.println("Conflict when creating e1: " + ex.getConflicts());
        }

        // create a conflicting event
        Instant e2Start = Instant.parse("2025-12-20T09:30:00Z");
        Instant e2End = Instant.parse("2025-12-20T10:30:00Z");
        Event e2 = Event.builder()
                .calendarId(cal.getId()).title("Standup")
                .start(e2Start).end(e2End)
                .description("Daily standup")
                .build();
        try {
            eventService.createEvent(e2);
            System.out.println("Created event e2");
        } catch (EventService.ConflictException ex) {
            System.out.println("Expected conflict for e2: " + ex.getConflicts());
            System.out.println("Showing available slots for duration of e2:");
            Duration dur = Duration.between(e2.getStart(), e2.getEnd());
            Instant windowEnd = e2.getStart().plus(Duration.ofDays(1));
            List<TimeSlot> slots = eventService.findAvailableSlots(cal.getId(), Instant.parse("2025-12-20T00:00:00Z"), windowEnd, dur);
            slots.forEach(System.out::println);

            System.out.println("Trying strategy to find next available slot and create shifted event.");
            FindNextAvailableStrategy strategy = new FindNextAvailableStrategy(eventService, e2.getStart().plus(Duration.ofDays(7)));
            try {
                Event shifted = strategy.apply(e2);
                Event storedShifted = eventService.createEvent(shifted);
                System.out.println("Created shifted event: " + storedShifted);
            } catch (Exception e) {
                System.out.println("Could not find slot: " + e.getMessage());
            }
        }

        // create recurring event: weekly on Monday for 3 occurrences
        ZonedDateTime rdStart = ZonedDateTime.of(LocalDateTime.of(2025, 12, 22, 9, 0), ZoneOffset.UTC);
        ZonedDateTime rdEnd = rdStart.plusHours(1);
        RecurrenceRule rr = new RecurrenceRule(RecurrenceRule.Frequency.WEEKLY, 1, 3, null, EnumSet.of(DayOfWeek.MONDAY));
        Event recurring = Event.builder()
                .calendarId(cal.getId()).title("Weekly Sync")
                .start(rdStart.toInstant()).end(rdEnd.toInstant())
                .recurrence(rr)
                .description("Weekly team sync")
                .build();
        try {
            eventService.createEvent(recurring);
            System.out.println("Created recurring event");
        } catch (EventService.ConflictException ex) {
            System.out.println("Conflict creating recurring: " + ex.getConflicts());
        }

        // list events for 2025-12-20 day
        Instant dayStart = Instant.parse("2025-12-20T00:00:00Z");
        Instant dayEnd = Instant.parse("2025-12-20T23:59:59Z");
        List<Event> events = eventService.listEvents(cal.getId(), dayStart, dayEnd);
        System.out.println("Events on 2025-12-20:");
        events.forEach(System.out::println);

        // list events for week containing 2025-12-22
        Instant wkStart = Instant.parse("2025-12-22T00:00:00Z");
        Instant wkEnd = Instant.parse("2025-12-28T23:59:59Z");
        List<Event> weekEvents = eventService.listEvents(cal.getId(), wkStart, wkEnd);
        System.out.println("Events for week:");
        weekEvents.forEach(System.out::println);
    }
}

