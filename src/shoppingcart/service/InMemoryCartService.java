package shoppingcart.service;

import shoppingcart.model.Cart;
import shoppingcart.model.Product;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCartService implements CartService {
    // ConcurrentHashMap, not LinkedHashMap: computeIfAbsent below relies on
    // ConcurrentHashMap's guarantee that the mapping function runs
    // atomically per key -- two concurrent getOrCreateCart calls for a
    // brand-new customer must not construct and register two different
    // Cart objects for the same customer.
    private final Map<String, Cart> cartsByCustomerId = new ConcurrentHashMap<>();

    @Override
    public Cart getOrCreateCart(String customerId) {
        return cartsByCustomerId.computeIfAbsent(customerId, Cart::new);
    }

    @Override
    public void addItem(String customerId, Product product, int quantity) {
        getOrCreateCart(customerId).addItem(product, quantity);
    }

    @Override
    public void updateQuantity(String customerId, String productId, int quantity) {
        getOrCreateCart(customerId).updateQuantity(productId, quantity);
    }

    @Override
    public void removeItem(String customerId, String productId) {
        getOrCreateCart(customerId).removeItem(productId);
    }

    @Override
    public void clearCart(String customerId) {
        getOrCreateCart(customerId).clear();
    }
}
