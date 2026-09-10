package library.model;

import java.time.LocalDate;

public class Loan {
    private final String id;
    private final BookItem bookItem;
    private final Member member;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;

    public Loan(String id, BookItem bookItem, Member member, LocalDate checkoutDate, LocalDate dueDate) {
        this.id = id;
        this.bookItem = bookItem;
        this.member = member;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public BookItem getBookItem() { return bookItem; }
    public Member getMember() { return member; }
    public LocalDate getCheckoutDate() { return checkoutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public synchronized LocalDate getReturnDate() { return returnDate; }
    public synchronized boolean isReturned() { return returnDate != null; }

    // Atomic "mark returned if not already" -- the isReturned check and the
    // write happen under one monitor, so two concurrent returnBook calls
    // for the same loan (e.g. a double-submitted request) can't both
    // succeed and, worse, both trigger a fine calculation / copy release.
    public synchronized boolean tryMarkReturned(LocalDate returnDate) {
        if (isReturned()) return false;
        this.returnDate = returnDate;
        return true;
    }

    @Override
    public String toString() {
        return "Loan{" + id + ", " + member.getName() + ", " + bookItem.getBook().getTitle()
                + ", due=" + dueDate + ", returned=" + returnDate + '}';
    }
}
