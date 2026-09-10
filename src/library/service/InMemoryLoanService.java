package library.service;

import library.model.BookItem;
import library.model.Loan;
import library.model.Member;
import library.strategy.FineStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryLoanService implements LoanService {
    private static final int LOAN_PERIOD_DAYS = 14;

    private final Map<String, Loan> loansByBarcode = new ConcurrentHashMap<>();
    private final CatalogService catalogService;
    private final FineStrategy fineStrategy;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryLoanService(CatalogService catalogService, FineStrategy fineStrategy) {
        this.catalogService = catalogService;
        this.fineStrategy = fineStrategy;
    }

    // No service-wide lock: scans this ISBN's copies (a read, safe under
    // concurrent modification) and attempts BookItem.tryLoan() -- an
    // atomic per-copy claim -- on each candidate, moving to the next copy
    // if another thread won the race on this one. Checkouts for a
    // DIFFERENT ISBN never contend with this at all, unlike locking the
    // whole service would cause.
    @Override
    public Loan checkout(Member member, String isbn, LocalDate checkoutDate) throws NoAvailableCopyException {
        for (BookItem item : catalogService.getItemsForIsbn(isbn)) {
            if (item.tryLoan()) {
                Loan loan = new Loan("LOAN-" + idCounter.getAndIncrement(), item, member, checkoutDate, checkoutDate.plusDays(LOAN_PERIOD_DAYS));
                loansByBarcode.put(item.getBarcode(), loan);
                return loan;
            }
        }
        throw new NoAvailableCopyException("No available copy for ISBN " + isbn);
    }

    @Override
    public double returnBook(String barcode, LocalDate returnDate) throws LoanNotFoundException {
        Loan loan = loansByBarcode.get(barcode);
        if (loan == null || !loan.tryMarkReturned(returnDate)) {
            throw new LoanNotFoundException("No active loan for barcode " + barcode);
        }
        loan.getBookItem().tryReturn();
        return fineStrategy.calculateFine(loan, returnDate);
    }

    @Override
    public List<Loan> getActiveLoansForMember(String memberId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loansByBarcode.values()) {
            if (!loan.isReturned() && loan.getMember().getId().equals(memberId)) result.add(loan);
        }
        return result;
    }
}
