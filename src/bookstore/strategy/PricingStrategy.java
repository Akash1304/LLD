package bookstore.strategy;

import bookstore.model.OrderItem;

import java.util.List;

public interface PricingStrategy {
    double calculateTotal(List<OrderItem> items);
}
