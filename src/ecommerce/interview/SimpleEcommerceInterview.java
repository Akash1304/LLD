package ecommerce.interview;

import java.util.*;

// Compact, single-file interview-friendly e-commerce demo.
// Supports: stock-checked order placement with a discount and a payment
// step, and cancellation that restores stock.
public class SimpleEcommerceInterview {

    static class Product {
        final String id;
        final String name;
        final double price;
        int stock;
        Product(String id, String name, double price, int stock) {
            this.id = id; this.name = name; this.price = price; this.stock = stock;
        }
        @Override public String toString() { return name + " ($" + price + ", stock=" + stock + ")"; }
    }

    static class Order {
        final String id;
        final Map<Product, Integer> items;
        final double total;
        boolean cancelled = false;
        Order(String id, Map<Product, Integer> items, double total) { this.id = id; this.items = items; this.total = total; }
    }

    final List<Product> catalog = new ArrayList<>();
    final Map<String, Order> orders = new HashMap<>();
    int counter = 1;

    void addProduct(Product p) { catalog.add(p); }

    Order placeOrder(Map<Product, Integer> cart, double discountPercent) throws Exception {
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            if (e.getKey().stock < e.getValue()) {
                throw new Exception("Insufficient stock for " + e.getKey().name);
            }
        }
        double subtotal = 0;
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            e.getKey().stock -= e.getValue();
            subtotal += e.getKey().price * e.getValue();
        }
        double total = subtotal * (1 - discountPercent / 100.0);
        Order order = new Order("ORD-" + counter++, cart, total);
        orders.put(order.id, order);
        System.out.println("  Charged $" + String.format("%.2f", total));
        return order;
    }

    void cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null || order.cancelled) return;
        order.cancelled = true;
        for (Map.Entry<Product, Integer> e : order.items.entrySet()) e.getKey().stock += e.getValue();
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleEcommerceInterview store = new SimpleEcommerceInterview();
        System.out.println("== Simple E-commerce Interview Demo ==");

        Product mouse = new Product("P1", "Wireless Mouse", 25.0, 10);
        Product keyboard = new Product("P2", "Mechanical Keyboard", 80.0, 2);
        store.addProduct(mouse);
        store.addProduct(keyboard);

        Map<Product, Integer> cart = new LinkedHashMap<>();
        cart.put(mouse, 1);
        cart.put(keyboard, 1);
        try {
            System.out.println("Placing order with a 10% discount:");
            Order order = store.placeOrder(cart, 10);
            System.out.println("Order " + order.id + " placed, total=$" + String.format("%.2f", order.total));
        } catch (Exception e) {
            System.out.println("Order failed: " + e.getMessage());
        }

        System.out.println("\nTrying to order 5 keyboards (only 1 left):");
        Map<Product, Integer> overCart = new LinkedHashMap<>();
        overCart.put(keyboard, 5);
        try {
            store.placeOrder(overCart, 0);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nCancelling the first order restores stock:");
        store.cancelOrder("ORD-1");
        System.out.println("Keyboard stock now: " + keyboard.stock);
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
