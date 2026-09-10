package ecommerce.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Address shippingAddress;
    private final double totalAmount;
    private final String paymentMethod;
    private OrderStatus status;

    private Order(Builder b) {
        this.id = b.id;
        this.customerId = b.customerId;
        this.items = new ArrayList<>(b.items);
        this.shippingAddress = b.shippingAddress;
        this.totalAmount = b.totalAmount;
        this.paymentMethod = b.paymentMethod;
        this.status = b.initialStatus;
    }

    public static Builder builder() { return new Builder(); }

    // Builder: seven args including two Strings next to each other
    // (customerId, paymentMethod) and a status that defaults to PLACED --
    // named setters plus a one-place validation (non-empty items,
    // non-negative total) replace an easy-to-misuse constructor.
    public static class Builder {
        private String id;
        private String customerId;
        private List<OrderItem> items = new ArrayList<>();
        private Address shippingAddress;
        private double totalAmount;
        private String paymentMethod;
        private OrderStatus initialStatus = OrderStatus.PLACED;

        public Builder id(String id) { this.id = id; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder items(List<OrderItem> items) { this.items = new ArrayList<>(items); return this; }
        public Builder shippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; return this; }
        public Builder totalAmount(double totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder initialStatus(OrderStatus initialStatus) { this.initialStatus = initialStatus; return this; }

        public Order build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(customerId, "customerId is required");
            Objects.requireNonNull(shippingAddress, "shippingAddress is required");
            Objects.requireNonNull(paymentMethod, "paymentMethod is required");
            if (items.isEmpty()) throw new IllegalArgumentException("an order needs at least one item");
            if (totalAmount < 0) throw new IllegalArgumentException("totalAmount cannot be negative");
            return new Order(this);
        }
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Address getShippingAddress() { return shippingAddress; }
    public double getTotalAmount() { return totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public synchronized OrderStatus getStatus() { return status; }

    // Compare-and-set for order status: shipOrder/deliverOrder/cancelOrder
    // all go through this, so e.g. a shipment racing a cancellation on the
    // same order can't both succeed and leave inconsistent side effects
    // (shipped-but-also-stock-restored).
    public synchronized boolean tryTransition(OrderStatus expected, OrderStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }

    // Used only by cancelOrder, which accepts cancellation from either of
    // two prior states (PLACED or PAID) -- a two-branch variant of
    // tryTransition since a single "expected" value doesn't cover it.
    public synchronized boolean tryCancelFrom(OrderStatus firstAllowed, OrderStatus secondAllowed, OrderStatus next) {
        if (status != firstAllowed && status != secondAllowed) return false;
        status = next;
        return true;
    }

    @Override
    public String toString() {
        return "Order{" + id + ", " + customerId + ", $" + String.format("%.2f", totalAmount)
                + ", " + paymentMethod + ", " + status + '}';
    }
}
