package bookstore.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String id;
    private final Customer customer;
    private final List<OrderItem> items;
    private final double total;
    private final Instant placedAt;
    private OrderStatus status;

    public Order(String id, Customer customer, List<OrderItem> items, double total) {
        this.id = id;
        this.customer = customer;
        this.items = items;
        this.total = total;
        this.placedAt = Instant.now();
        this.status = OrderStatus.PLACED;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public double getTotal() { return total; }
    public Instant getPlacedAt() { return placedAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Order{" + id + ", " + customer.getName() + ", total=$" + total + ", " + status + '}';
    }
}
