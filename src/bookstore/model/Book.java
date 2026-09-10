package bookstore.model;

public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final String subject;
    private final double price;
    private int stock;

    public Book(String isbn, String title, String author, String subject, double price, int stock) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.subject = subject;
        this.price = price;
        this.stock = stock;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getSubject() { return subject; }
    public double getPrice() { return price; }
    public synchronized int getStock() { return stock; }

    public synchronized void adjustStock(int delta) {
        int updated = stock + delta;
        if (updated < 0) throw new IllegalStateException("Insufficient stock for " + isbn);
        stock = updated;
    }

    // Atomic "reserve if enough stock" primitive, one monitor per Book: the
    // check (enough stock?) and the act (decrement) happen under the same
    // lock, so two threads reserving the last copies concurrently can't
    // both pass the check the way a separate getStock()-then-adjustStock()
    // pair could. This is what InventoryService.reserveStock delegates to,
    // instead of locking the whole service (and therefore every other
    // book) just to touch one book's count.
    public synchronized boolean tryReserve(int quantity) {
        if (stock < quantity) return false;
        stock -= quantity;
        return true;
    }

    @Override
    public String toString() {
        return "Book{" + isbn + ", '" + title + "' by " + author + ", $" + price + ", stock=" + stock + '}';
    }
}
