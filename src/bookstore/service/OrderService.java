package bookstore.service;

import bookstore.model.Customer;
import bookstore.model.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(Customer customer, Map<String, Integer> isbnToQuantity) throws InsufficientStockException;
    Optional<Order> cancelOrder(String orderId);
    List<Order> getOrdersForCustomer(String customerId);

    class InsufficientStockException extends Exception {
        public InsufficientStockException(String message) { super(message); }
    }
}
