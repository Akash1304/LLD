package shoppingcart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cart {
    private final String customerId;
    private final Map<String, CartItem> itemsByProductId = new LinkedHashMap<>();

    public Cart(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() { return customerId; }

    // Every method touching itemsByProductId is synchronized on this Cart
    // instance -- one monitor per customer's cart, so editing customer A's
    // cart from one device never blocks customer B's cart, but two
    // concurrent edits to the SAME cart (e.g. two open tabs) can't
    // interleave into a corrupted LinkedHashMap or a lost update (like
    // both reading the same CartItem's quantity before either writes it
    // back).
    public synchronized void addItem(Product product, int quantity) {
        CartItem existing = itemsByProductId.get(product.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            itemsByProductId.put(product.getId(), new CartItem(product, quantity));
        }
    }

    public synchronized void updateQuantity(String productId, int quantity) {
        if (quantity <= 0) {
            itemsByProductId.remove(productId);
            return;
        }
        CartItem item = itemsByProductId.get(productId);
        if (item == null) throw new IllegalArgumentException("Product not in cart: " + productId);
        item.setQuantity(quantity);
    }

    public synchronized void removeItem(String productId) {
        itemsByProductId.remove(productId);
    }

    public synchronized void clear() {
        itemsByProductId.clear();
    }

    public synchronized boolean isEmpty() { return itemsByProductId.isEmpty(); }

    public synchronized List<CartItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(itemsByProductId.values()));
    }

    public synchronized double getSubtotal() {
        return itemsByProductId.values().stream().mapToDouble(CartItem::getLineTotal).sum();
    }

    @Override
    public synchronized String toString() { return "Cart{" + customerId + ", " + getItems() + ", subtotal=$" + getSubtotal() + '}'; }
}
