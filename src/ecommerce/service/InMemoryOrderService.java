package ecommerce.service;

import ecommerce.model.Address;
import ecommerce.model.Order;
import ecommerce.model.OrderItem;
import ecommerce.model.OrderStatus;
import ecommerce.model.Product;
import ecommerce.strategy.DiscountStrategy;
import ecommerce.strategy.PaymentStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderService implements OrderService {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final ProductCatalogService catalogService;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryOrderService(ProductCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public Order placeOrder(String customerId, Map<String, Integer> productQuantities, Address shippingAddress, PaymentStrategy paymentStrategy, DiscountStrategy discountStrategy) throws OrderPlacementException {
        List<OrderItem> items = new ArrayList<>();
        List<Product> reserved = new ArrayList<>();
        List<Integer> reservedQuantities = new ArrayList<>();

        try {
            for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
                Product product = catalogService.getProduct(entry.getKey())
                        .orElseThrow(() -> new OrderPlacementException("Unknown product: " + entry.getKey()));
                int qty = entry.getValue();
                // tryReserve is the atomic check-and-decrement; a separate
                // getStock()-then-adjustStock() pair (the previous
                // implementation) is a check-then-act race under
                // concurrent orders for the last units of a product.
                if (!product.tryReserve(qty)) {
                    throw new OrderPlacementException("Insufficient stock for " + product.getName() + " (wanted " + qty + ", have " + product.getStock() + ")");
                }
                reserved.add(product);
                reservedQuantities.add(qty);
                items.add(new OrderItem(product, qty));
            }

            double subtotal = items.stream().mapToDouble(OrderItem::getLineTotal).sum();
            double total = discountStrategy.apply(subtotal);

            try {
                paymentStrategy.pay(total);
            } catch (PaymentStrategy.PaymentFailedException e) {
                throw new OrderPlacementException("Payment failed: " + e.getMessage());
            }

            OrderStatus initialStatus = paymentStrategy.isPayNow() ? OrderStatus.PAID : OrderStatus.PLACED;
            Order order = Order.builder()
                    .id("ORD-" + idCounter.getAndIncrement())
                    .customerId(customerId).items(items)
                    .shippingAddress(shippingAddress).totalAmount(total)
                    .paymentMethod(paymentStrategy.getName()).initialStatus(initialStatus)
                    .build();
            orders.put(order.getId(), order);
            return order;
        } catch (OrderPlacementException e) {
            for (int i = 0; i < reserved.size(); i++) {
                reserved.get(i).adjustStock(reservedQuantities.get(i));
            }
            throw e;
        }
    }

    @Override
    public Order shipOrder(String orderId) throws InvalidOrderStateException {
        Order order = requireOrder(orderId);
        if (!order.tryTransition(OrderStatus.PAID, OrderStatus.SHIPPED)) {
            throw new InvalidOrderStateException("Cannot ship order in state " + order.getStatus());
        }
        return order;
    }

    @Override
    public Order deliverOrder(String orderId) throws InvalidOrderStateException {
        Order order = requireOrder(orderId);
        if (!order.tryTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            throw new InvalidOrderStateException("Cannot deliver order in state " + order.getStatus());
        }
        return order;
    }

    @Override
    public Optional<Order> cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null || !order.tryCancelFrom(OrderStatus.PLACED, OrderStatus.PAID, OrderStatus.CANCELLED)) {
            return Optional.empty();
        }
        for (OrderItem item : order.getItems()) {
            item.getProduct().adjustStock(item.getQuantity());
        }
        return Optional.of(order);
    }

    @Override
    public List<Order> getOrdersForCustomer(String customerId) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders.values()) {
            if (o.getCustomerId().equals(customerId)) result.add(o);
        }
        return result;
    }

    private Order requireOrder(String orderId) throws InvalidOrderStateException {
        Order order = orders.get(orderId);
        if (order == null) throw new InvalidOrderStateException("Unknown order: " + orderId);
        return order;
    }
}
