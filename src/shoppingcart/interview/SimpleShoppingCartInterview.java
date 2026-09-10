package shoppingcart.interview;

import java.util.*;

// Compact, single-file interview-friendly shopping cart demo.
// Supports: add/update/remove line items, a subtotal, a minimum-spend
// discount, and checkout that clears the cart.
public class SimpleShoppingCartInterview {

    static class Product {
        final String id;
        final String name;
        final double price;
        Product(String id, String name, double price) { this.id = id; this.name = name; this.price = price; }
    }

    static class CartItem {
        final Product product;
        int quantity;
        CartItem(Product product, int quantity) { this.product = product; this.quantity = quantity; }
        double lineTotal() { return product.price * quantity; }
        @Override public String toString() { return quantity + "x " + product.name; }
    }

    final Map<String, CartItem> items = new LinkedHashMap<>();

    void addItem(Product product, int quantity) {
        items.merge(product.id, new CartItem(product, quantity), (existing, added) -> {
            existing.quantity += added.quantity;
            return existing;
        });
    }

    void updateQuantity(String productId, int quantity) {
        if (quantity <= 0) { items.remove(productId); return; }
        items.get(productId).quantity = quantity;
    }

    void removeItem(String productId) { items.remove(productId); }

    double subtotal() { return items.values().stream().mapToDouble(CartItem::lineTotal).sum(); }

    double checkout(double minimumSpend, double flatDiscount) throws Exception {
        if (items.isEmpty()) throw new Exception("Cannot check out an empty cart");
        double subtotal = subtotal();
        double total = subtotal >= minimumSpend ? subtotal - flatDiscount : subtotal;
        items.clear();
        return total;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleShoppingCartInterview cart = new SimpleShoppingCartInterview();
        System.out.println("== Simple Shopping Cart Interview Demo ==");

        Product mouse = new Product("P1", "Wireless Mouse", 25.0);
        Product keyboard = new Product("P2", "Mechanical Keyboard", 80.0);

        cart.addItem(mouse, 2);
        cart.addItem(keyboard, 1);
        System.out.println("Cart: " + cart.items.values() + ", subtotal=$" + cart.subtotal());

        System.out.println("\nReducing mouse quantity to 1:");
        cart.updateQuantity("P1", 1);
        System.out.println("Cart: " + cart.items.values() + ", subtotal=$" + cart.subtotal());

        try {
            System.out.println("\nChecking out with a '$15 off orders over $50' promo:");
            double total = cart.checkout(50.0, 15.0);
            System.out.printf("Total charged: $%.2f%n", total);

            System.out.println("\nTrying to check out again with an empty cart:");
            try {
                cart.checkout(50.0, 15.0);
            } catch (Exception e) {
                System.out.println("Expected failure: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
