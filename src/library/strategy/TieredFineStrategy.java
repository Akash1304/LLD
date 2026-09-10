package library.strategy;

import library.model.Loan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// First 7 late days are cheap; anything beyond that escalates, to push
// members to return (or renew) promptly instead of sitting on a fine.
public class TieredFineStrategy implements FineStrategy {
    private static final int GRACE_ESCALATION_DAY = 7;
    private static final double RATE_PER_DAY_TIER_1 = 0.25;
    private static final double RATE_PER_DAY_TIER_2 = 1.00;

    @Override
    public double calculateFine(Loan loan, LocalDate asOf) {
        long lateDays = ChronoUnit.DAYS.between(loan.getDueDate(), asOf);
        if (lateDays <= 0) return 0.0;
        long tier1Days = Math.min(lateDays, GRACE_ESCALATION_DAY);
        long tier2Days = Math.max(0, lateDays - GRACE_ESCALATION_DAY);
        return tier1Days * RATE_PER_DAY_TIER_1 + tier2Days * RATE_PER_DAY_TIER_2;
    }
}
