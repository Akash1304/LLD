package calendar.service;

import calendar.model.Calendar;
import calendar.model.User;

import java.util.List;
import java.util.Optional;

public interface CalendarService {
    Calendar createCalendar(User owner);
    List<Calendar> listCalendars(User owner);
    Optional<Calendar> getCalendar(String id);
    boolean deleteCalendar(String id);
}

