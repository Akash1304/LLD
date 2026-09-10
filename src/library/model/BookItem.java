package library.model;

public class BookItem {
    private final String barcode;
    private final Book book;
    private BookItemStatus status;

    public BookItem(String barcode, Book book) {
        this.barcode = barcode;
        this.book = book;
        this.status = BookItemStatus.AVAILABLE;
    }

    public String getBarcode() { return barcode; }
    public Book getBook() { return book; }
    public synchronized BookItemStatus getStatus() { return status; }
    public synchronized void setStatus(BookItemStatus status) { this.status = status; }

    // Atomic "claim if available" -- the check and the status flip happen
    // under the same monitor (one per copy), so two members racing to
    // check out the same physical copy can't both succeed. LoanService
    // scans a title's copies trying this on each until one succeeds,
    // instead of a separate "find available"-then-"mark loaned" pair.
    public synchronized boolean tryLoan() {
        if (status != BookItemStatus.AVAILABLE) return false;
        status = BookItemStatus.LOANED;
        return true;
    }

    // Symmetric atomic "return if loaned" primitive.
    public synchronized boolean tryReturn() {
        if (status != BookItemStatus.LOANED) return false;
        status = BookItemStatus.AVAILABLE;
        return true;
    }

    @Override
    public String toString() { return barcode + " (" + book.getTitle() + ", " + status + ")"; }
}
