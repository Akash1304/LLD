package library.service;

import library.model.Loan;
import library.model.Member;

import java.time.LocalDate;
import java.util.List;

public interface LoanService {
    Loan checkout(Member member, String isbn, LocalDate checkoutDate) throws NoAvailableCopyException;
    double returnBook(String barcode, LocalDate returnDate) throws LoanNotFoundException;
    List<Loan> getActiveLoansForMember(String memberId);

    class NoAvailableCopyException extends Exception {
        public NoAvailableCopyException(String message) { super(message); }
    }

    class LoanNotFoundException extends Exception {
        public LoanNotFoundException(String message) { super(message); }
    }
}
