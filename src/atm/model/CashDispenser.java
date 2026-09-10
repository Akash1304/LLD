package atm.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class CashDispenser {
    // denomination -> count of bills available, e.g. 100 -> 5 means five $100 bills
    private final TreeMap<Integer, Integer> denominationCounts = new TreeMap<>(java.util.Collections.reverseOrder());

    // One CashDispenser can plausibly be shared -- a bank of ATMs drawing
    // from one vault, or a refill/audit process running while withdrawals
    // are in flight -- so every method that reads or mutates
    // denominationCounts is synchronized on this instance. dispense() in
    // particular is a compound "plan a breakdown, then deduct it" sequence
    // across the whole map; without the lock, two concurrent withdrawals
    // could both plan against the same not-yet-deducted counts and
    // together dispense more bills of a denomination than physically exist.
    public synchronized void loadCash(int denomination, int count) {
        denominationCounts.merge(denomination, count, Integer::sum);
    }

    public synchronized int getTotalCash() {
        int total = 0;
        for (Map.Entry<Integer, Integer> e : denominationCounts.entrySet()) total += e.getKey() * e.getValue();
        return total;
    }

    // greedy largest-denomination-first breakdown; throws if the amount
    // can't be represented exactly with the bills currently available
    public synchronized Map<Integer, Integer> dispense(int amount) {
        if (amount % smallestDenomination() != 0) {
            throw new IllegalArgumentException("Amount must be a multiple of " + smallestDenomination());
        }
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        int remaining = amount;
        for (Map.Entry<Integer, Integer> entry : denominationCounts.entrySet()) {
            int denom = entry.getKey();
            int available = entry.getValue();
            int needed = Math.min(available, remaining / denom);
            if (needed > 0) {
                plan.put(denom, needed);
                remaining -= needed * denom;
            }
        }
        if (remaining != 0) {
            throw new IllegalStateException("Cannot dispense $" + amount + " with available denominations");
        }
        plan.forEach((denom, count) -> denominationCounts.merge(denom, -count, Integer::sum));
        return plan;
    }

    private int smallestDenomination() {
        return denominationCounts.isEmpty() ? 1 : denominationCounts.lastKey();
    }
}
