package calendar.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

public class RecurrenceRule {
    public enum Frequency { DAILY, WEEKLY, MONTHLY }

    private final Frequency frequency;
    private final int interval; // every N frequency units
    private final Integer count; // optional
    private final LocalDateTime until; // optional
    private final Set<DayOfWeek> byDay; // for weekly rules

    public RecurrenceRule(Frequency frequency, int interval, Integer count, LocalDateTime until, Set<DayOfWeek> byDay) {
        this.frequency = frequency;
        this.interval = interval;
        this.count = count;
        this.until = until;
        this.byDay = byDay;
    }

    public Frequency getFrequency() { return frequency; }
    public int getInterval() { return interval; }
    public Integer getCount() { return count; }
    public LocalDateTime getUntil() { return until; }
    public Set<DayOfWeek> getByDay() { return byDay; }
}

