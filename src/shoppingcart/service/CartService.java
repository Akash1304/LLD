package shoppingcart.service;

import shoppingcart.model.Cart;
import shoppingcart.model.Product;

public interface CartService {
    Cart getOrCreateCart(String customerId);
    void addItem(String customerId, Product product, int quantity);
    void updateQuantity(String customerId, String productId, int quantity);
    void removeItem(String customerId, String productId);
    void clearCart(String customerId);
}
