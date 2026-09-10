package bookstore.strategy;

import bookstore.model.OrderItem;

import java.util.List;

public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public double calculateTotal(List<OrderItem> items) {
        return items.stream().mapToDouble(OrderItem::getLineTotal).sum();
    }
}
