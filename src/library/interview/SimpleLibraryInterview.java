package library.interview;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Compact, single-file interview-friendly library demo.
// Supports: multiple physical copies per title, checkout/return, and a
// flat per-day late fine.
public class SimpleLibraryInterview {
    static final int LOAN_PERIOD_DAYS = 14;
    static final double FINE_PER_DAY = 0.50;

    static class BookItem {
        final String barcode;
        final String title;
        boolean available = true;
        BookItem(String barcode, String title) { this.barcode = barcode; this.title = title; }
    }

    static class Loan {
        final BookItem item;
        final String member;
        final LocalDate dueDate;
        LocalDate returnDate;
        Loan(BookItem item, String member, LocalDate dueDate) { this.item = item; this.member = member; this.dueDate = dueDate; }
    }

    final List<BookItem> items = new ArrayList<>();
    final Map<String, Loan> activeLoans = new HashMap<>(); // barcode -> loan

    void addCopy(BookItem item) { items.add(item); }

    Optional<BookItem> findAvailable(String title) {
        return items.stream().filter(i -> i.title.equals(title) && i.available).findFirst();
    }

    Loan checkout(String title, String member, LocalDate today) throws Exception {
        BookItem item = findAvailable(title).orElseThrow(() -> new Exception("No available copy of " + title));
        item.available = false;
        Loan loan = new Loan(item, member, today.plusDays(LOAN_PERIOD_DAYS));
        activeLoans.put(item.barcode, loan);
        return loan;
    }

    double returnBook(String barcode, LocalDate today) throws Exception {
        Loan loan = activeLoans.remove(barcode);
        if (loan == null) throw new Exception("No active loan for " + barcode);
        loan.item.available = true;
        long lateDays = ChronoUnit.DAYS.between(loan.dueDate, today);
        return lateDays > 0 ? lateDays * FINE_PER_DAY : 0.0;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleLibraryInterview library = new SimpleLibraryInterview();
        System.out.println("== Simple Library Interview Demo ==");

        library.addCopy(new BookItem("BC-1", "Clean Code"));
        library.addCopy(new BookItem("BC-2", "Clean Code"));

        LocalDate today = LocalDate.of(2026, 1, 1);
        try {
            Loan aliceLoan = library.checkout("Clean Code", "Alice", today);
            System.out.println("Alice checked out " + aliceLoan.item.barcode + ", due " + aliceLoan.dueDate);
            Loan bobLoan = library.checkout("Clean Code", "Bob", today);
            System.out.println("Bob checked out " + bobLoan.item.barcode + ", due " + bobLoan.dueDate);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }

        System.out.println("\nTrying a third checkout (both copies are out):");
        try {
            library.checkout("Clean Code", "Carol", today);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nAlice returns her copy 5 days late:");
        try {
            double fine = library.returnBook("BC-1", today.plusDays(19));
            System.out.printf("Fine: $%.2f%n", fine);
        } catch (Exception e) {
            System.out.println("Return failed: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
