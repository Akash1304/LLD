package library.strategy;

import library.model.Loan;

public interface FineStrategy {
    double calculateFine(Loan loan, java.time.LocalDate asOf);
}
