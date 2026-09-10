package bookstore.interview;

import java.util.*;

// Compact, single-file interview-friendly bookstore demo.
// Supports: catalog search by author, placing an order with stock checks,
// and cancelling an order to restore stock.
public class SimpleBookstoreInterview {

    static class Book {
        final String isbn;
        final String title;
        final String author;
        int stock;
        final double price;
        Book(String isbn, String title, String author, double price, int stock) {
            this.isbn = isbn; this.title = title; this.author = author; this.price = price; this.stock = stock;
        }
        @Override public String toString() { return title + " by " + author + " ($" + price + ", stock=" + stock + ")"; }
    }

    static class Order {
        final String id;
        final Map<Book, Integer> items;
        final double total;
        boolean cancelled = false;
        Order(String id, Map<Book, Integer> items, double total) { this.id = id; this.items = items; this.total = total; }
    }

    final List<Book> catalog = new ArrayList<>();
    final Map<String, Order> orders = new HashMap<>();
    int orderCounter = 1;

    void addBook(Book b) { catalog.add(b); }

    List<Book> searchByAuthor(String author) {
        List<Book> found = new ArrayList<>();
        for (Book b : catalog) if (b.author.equalsIgnoreCase(author)) found.add(b);
        return found;
    }

    Order placeOrder(Map<Book, Integer> cart) throws Exception {
        for (Map.Entry<Book, Integer> e : cart.entrySet()) {
            if (e.getKey().stock < e.getValue()) {
                throw new Exception("Insufficient stock for " + e.getKey().title + ": wanted " + e.getValue() + ", have " + e.getKey().stock);
            }
        }
        double total = 0;
        for (Map.Entry<Book, Integer> e : cart.entrySet()) {
            e.getKey().stock -= e.getValue();
            total += e.getKey().price * e.getValue();
        }
        Order order = new Order("ORD-" + orderCounter++, cart, total);
        orders.put(order.id, order);
        return order;
    }

    void cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null || order.cancelled) return;
        order.cancelled = true;
        for (Map.Entry<Book, Integer> e : order.items.entrySet()) e.getKey().stock += e.getValue();
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleBookstoreInterview store = new SimpleBookstoreInterview();
        System.out.println("== Simple Bookstore Interview Demo ==");

        Book effectiveJava = new Book("978-1", "Effective Java", "Joshua Bloch", 45.0, 10);
        Book cleanCode = new Book("978-2", "Clean Code", "Robert Martin", 40.0, 3);
        store.addBook(effectiveJava);
        store.addBook(cleanCode);

        System.out.println("Searching for author 'Robert Martin': " + store.searchByAuthor("Robert Martin"));

        Map<Book, Integer> cart = new LinkedHashMap<>();
        cart.put(effectiveJava, 2);
        cart.put(cleanCode, 2);
        try {
            Order order = store.placeOrder(cart);
            System.out.println("Placed " + order.id + ", total=$" + order.total);
        } catch (Exception e) {
            System.out.println("Order failed: " + e.getMessage());
        }

        System.out.println("\nTrying to over-order Clean Code (only " + cleanCode.stock + " left):");
        Map<Book, Integer> overCart = new LinkedHashMap<>();
        overCart.put(cleanCode, 5);
        try {
            store.placeOrder(overCart);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nCancelling first order:");
        store.cancelOrder("ORD-1");
        System.out.println("Clean Code stock after cancellation: " + cleanCode.stock);
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
