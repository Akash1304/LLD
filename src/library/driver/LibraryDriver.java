package library.driver;

import library.model.Book;
import library.model.BookItem;
import library.model.Loan;
import library.model.Member;
import library.service.CatalogService;
import library.service.InMemoryCatalogService;
import library.service.InMemoryLoanService;
import library.service.LoanService;
import library.strategy.TieredFineStrategy;

import java.time.LocalDate;
import java.util.List;

public class LibraryDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        CatalogService catalogService = new InMemoryCatalogService();
        LoanService loanService = new InMemoryLoanService(catalogService, new TieredFineStrategy());

        Book cleanCode = catalogService.addBook(new Book("978-1", "Clean Code", "Robert Martin"));
        catalogService.addBookItem(new BookItem("BC-1", cleanCode));
        catalogService.addBookItem(new BookItem("BC-2", cleanCode));

        Member alice = new Member("M1", "Alice");
        Member bob = new Member("M2", "Bob");

        LocalDate today = LocalDate.of(2026, 1, 1);

        System.out.println("Searching for title 'Clean': " + catalogService.searchByTitle("Clean"));

        try {
            Loan aliceLoan = loanService.checkout(alice, "978-1", today);
            System.out.println("Alice checked out: " + aliceLoan);
            Loan bobLoan = loanService.checkout(bob, "978-1", today);
            System.out.println("Bob checked out: " + bobLoan);
        } catch (LoanService.NoAvailableCopyException e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }

        System.out.println("\nTrying a third checkout (both copies are out):");
        try {
            loanService.checkout(new Member("M3", "Carol"), "978-1", today);
        } catch (LoanService.NoAvailableCopyException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nAlice returns her copy 20 days late (due after 14 days, so 6 days overdue):");
        try {
            double fine = loanService.returnBook("BC-1", today.plusDays(20));
            System.out.printf("Fine: $%.2f%n", fine);
        } catch (LoanService.LoanNotFoundException e) {
            System.out.println("Return failed: " + e.getMessage());
        }

        System.out.println("\nActive loans for Bob: " + loanService.getActiveLoansForMember("M2"));

        List<BookItem> items = catalogService.getItemsForIsbn("978-1");
        System.out.println("\nAll copies of Clean Code: " + items);
    }
}
