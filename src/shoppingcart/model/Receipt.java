package shoppingcart.model;

import java.time.Instant;
import java.util.List;

public class Receipt {
    private final String id;
    private final String customerId;
    private final List<CartItem> items;
    private final double subtotal;
    private final double total;
    private final Instant checkedOutAt;

    public Receipt(String id, String customerId, List<CartItem> items, double subtotal, double total) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.subtotal = subtotal;
        this.total = total;
        this.checkedOutAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<CartItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getTotal() { return total; }
    public double getDiscountApplied() { return subtotal - total; }
    public Instant getCheckedOutAt() { return checkedOutAt; }

    @Override
    public String toString() {
        return "Receipt{" + id + ", " + customerId + ", subtotal=$" + String.format("%.2f", subtotal)
                + ", discount=$" + String.format("%.2f", getDiscountApplied())
                + ", total=$" + String.format("%.2f", total) + '}';
    }
}
