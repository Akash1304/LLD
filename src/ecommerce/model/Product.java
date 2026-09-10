package ecommerce.model;

public class Product {
    private final String id;
    private final String name;
    private final String category;
    private final double price;
    private int stock;

    public Product(String id, String name, String category, double price, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public synchronized int getStock() { return stock; }

    public synchronized void adjustStock(int delta) {
        int updated = stock + delta;
        if (updated < 0) throw new IllegalStateException("Insufficient stock for " + id);
        stock = updated;
    }

    // Atomic "reserve if enough stock" -- the check and the decrement
    // happen under this product's own monitor, so two concurrent orders
    // for the last units of the same product can't both pass the check
    // the way a separate getStock()-then-adjustStock() pair could
    // (exactly this race, previously present in InMemoryOrderService).
    // Mirrors Book.tryReserve in the Bookstore LLD.
    public synchronized boolean tryReserve(int quantity) {
        if (stock < quantity) return false;
        stock -= quantity;
        return true;
    }

    @Override
    public String toString() { return name + " ($" + price + ", " + category + ", stock=" + stock + ")"; }
}
