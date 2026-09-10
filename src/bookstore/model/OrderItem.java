package bookstore.model;

public class OrderItem {
    private final Book book;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(Book book, int quantity) {
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = book.getPrice();
    }

    public Book getBook() { return book; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getLineTotal() { return unitPrice * quantity; }

    @Override
    public String toString() {
        return quantity + "x " + book.getTitle() + " @ $" + unitPrice;
    }
}
