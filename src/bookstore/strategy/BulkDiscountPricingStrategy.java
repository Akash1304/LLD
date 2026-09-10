package bookstore.strategy;

import bookstore.model.OrderItem;

import java.util.List;

public class BulkDiscountPricingStrategy implements PricingStrategy {
    private static final int DISCOUNT_THRESHOLD_QTY = 5;
    private static final double DISCOUNT_RATE = 0.10;

    @Override
    public double calculateTotal(List<OrderItem> items) {
        double subtotal = items.stream().mapToDouble(OrderItem::getLineTotal).sum();
        int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
        return totalQty >= DISCOUNT_THRESHOLD_QTY ? subtotal * (1 - DISCOUNT_RATE) : subtotal;
    }
}
