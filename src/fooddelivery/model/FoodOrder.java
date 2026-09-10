package fooddelivery.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FoodOrder {
    private final String id;
    private final String customerId;
    private final Restaurant restaurant;
    private final Location deliveryLocation;
    private final List<FoodOrderItem> items;
    private final double totalPrice;
    private OrderStatus status;
    private DeliveryAgent assignedAgent;

    private FoodOrder(Builder b) {
        this.id = b.id;
        this.customerId = b.customerId;
        this.restaurant = b.restaurant;
        this.deliveryLocation = b.deliveryLocation;
        this.items = new ArrayList<>(b.items);
        this.totalPrice = b.totalPrice;
        this.status = OrderStatus.PLACED;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String customerId;
        private Restaurant restaurant;
        private Location deliveryLocation;
        private List<FoodOrderItem> items = new ArrayList<>();
        private double totalPrice;

        public Builder id(String id) { this.id = id; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder restaurant(Restaurant restaurant) { this.restaurant = restaurant; return this; }
        public Builder deliveryLocation(Location deliveryLocation) { this.deliveryLocation = deliveryLocation; return this; }
        public Builder items(List<FoodOrderItem> items) { this.items = new ArrayList<>(items); return this; }
        public Builder totalPrice(double totalPrice) { this.totalPrice = totalPrice; return this; }

        public FoodOrder build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(customerId, "customerId is required");
            Objects.requireNonNull(restaurant, "restaurant is required");
            Objects.requireNonNull(deliveryLocation, "deliveryLocation is required");
            if (items.isEmpty()) throw new IllegalArgumentException("an order needs at least one item");
            return new FoodOrder(this);
        }
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public Restaurant getRestaurant() { return restaurant; }
    public Location getDeliveryLocation() { return deliveryLocation; }
    public List<FoodOrderItem> getItems() { return Collections.unmodifiableList(items); }
    public double getTotalPrice() { return totalPrice; }
    public synchronized OrderStatus getStatus() { return status; }
    public synchronized DeliveryAgent getAssignedAgent() { return assignedAgent; }

    // Compare-and-set for status: markPreparing/assignDelivery/
    // completeDelivery/cancelOrder all go through this so that, e.g., two
    // concurrent cancellation-vs-assignment requests for the same order
    // can't both succeed. `assignedAgent` is set atomically alongside the
    // status transition to OUT_FOR_DELIVERY, under the same monitor, so a
    // reader can never observe OUT_FOR_DELIVERY with no agent attached.
    public synchronized boolean tryTransition(OrderStatus expected, OrderStatus next) {
        if (status != expected) return false;
        status = next;
        return true;
    }

    public synchronized boolean tryAssignAndTransition(DeliveryAgent agent, OrderStatus expected, OrderStatus next) {
        if (status != expected) return false;
        assignedAgent = agent;
        status = next;
        return true;
    }

    @Override
    public synchronized String toString() {
        return "FoodOrder{" + id + ", " + customerId + ", " + restaurant.getName()
                + ", $" + String.format("%.2f", totalPrice) + ", " + status
                + (assignedAgent != null ? ", agent=" + assignedAgent.getName() : "") + '}';
    }
}
