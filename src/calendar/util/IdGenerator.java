package calendar.util;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    private static final AtomicLong COUNTER = new AtomicLong(1);

    public static String nextId() {
        return String.valueOf(COUNTER.getAndIncrement());
    }
}

