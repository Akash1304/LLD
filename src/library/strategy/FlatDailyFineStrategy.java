package library.strategy;

import library.model.Loan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FlatDailyFineStrategy implements FineStrategy {
    private static final double RATE_PER_DAY = 0.50;

    @Override
    public double calculateFine(Loan loan, LocalDate asOf) {
        long lateDays = ChronoUnit.DAYS.between(loan.getDueDate(), asOf);
        return lateDays > 0 ? lateDays * RATE_PER_DAY : 0.0;
    }
}
