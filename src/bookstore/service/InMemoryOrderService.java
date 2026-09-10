package bookstore.service;

import bookstore.model.Book;
import bookstore.model.Customer;
import bookstore.model.Order;
import bookstore.model.OrderItem;
import bookstore.model.OrderStatus;
import bookstore.strategy.PricingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderService implements OrderService {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final PricingStrategy pricingStrategy;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryOrderService(CatalogService catalogService, InventoryService inventoryService, PricingStrategy pricingStrategy) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.pricingStrategy = pricingStrategy;
    }

    @Override
    public Order placeOrder(Customer customer, Map<String, Integer> isbnToQuantity) throws InsufficientStockException {
        List<String> reserved = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        try {
            for (Map.Entry<String, Integer> entry : isbnToQuantity.entrySet()) {
                String isbn = entry.getKey();
                int qty = entry.getValue();
                Book book = catalogService.getBook(isbn)
                        .orElseThrow(() -> new InsufficientStockException("Unknown ISBN: " + isbn));
                if (!inventoryService.reserveStock(isbn, qty)) {
                    throw new InsufficientStockException("Insufficient stock for " + isbn + " (wanted " + qty + ", have " + book.getStock() + ")");
                }
                reserved.add(isbn);
                items.add(new OrderItem(book, qty));
            }
        } catch (InsufficientStockException ex) {
            // roll back any reservations already made for this order
            for (int i = 0; i < reserved.size(); i++) {
                inventoryService.restock(reserved.get(i), items.get(i).getQuantity());
            }
            throw ex;
        }

        double total = pricingStrategy.calculateTotal(items);
        String orderId = "ORD-" + idCounter.getAndIncrement();
        Order order = new Order(orderId, customer, items, total);
        orders.put(orderId, order);
        return order;
    }

    @Override
    public Optional<Order> cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null || order.getStatus() != OrderStatus.PLACED) return Optional.empty();
        order.setStatus(OrderStatus.CANCELLED);
        for (OrderItem item : order.getItems()) {
            inventoryService.restock(item.getBook().getIsbn(), item.getQuantity());
        }
        return Optional.of(order);
    }

    @Override
    public List<Order> getOrdersForCustomer(String customerId) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders.values()) {
            if (o.getCustomer().getId().equals(customerId)) result.add(o);
        }
        return result;
    }
}
