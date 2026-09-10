package calendar.service;

import calendar.model.Calendar;
import calendar.model.User;
import calendar.util.IdGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryCalendarService implements CalendarService {
    // ConcurrentHashMap, not HashMap: IdGenerator hands out unique keys, so
    // puts never collide, but plain HashMap is still unsafe under
    // concurrent structural modification (e.g. two puts triggering an
    // internal resize at the same time can corrupt the bucket structure).
    private final Map<String, Calendar> calendars = new ConcurrentHashMap<>();

    @Override
    public Calendar createCalendar(User owner, String name) {
        String id = IdGenerator.nextId();
        Calendar c = new Calendar(id, owner, name);
        calendars.put(id, c);
        return c;
    }

    @Override
    public List<Calendar> listCalendars(User owner) {
        return calendars.values().stream().filter(c -> c.getOwner().equals(owner)).collect(Collectors.toList());
    }

    @Override
    public Optional<Calendar> getCalendar(String id) {
        return Optional.ofNullable(calendars.get(id));
    }

    @Override
    public boolean deleteCalendar(String id) {
        return calendars.remove(id) != null;
    }
}

