package ecommerce.service;

import ecommerce.model.Address;
import ecommerce.model.Order;
import ecommerce.strategy.DiscountStrategy;
import ecommerce.strategy.PaymentStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    Order placeOrder(String customerId, Map<String, Integer> productQuantities, Address shippingAddress, PaymentStrategy paymentStrategy, DiscountStrategy discountStrategy) throws OrderPlacementException;
    Order shipOrder(String orderId) throws InvalidOrderStateException;
    Order deliverOrder(String orderId) throws InvalidOrderStateException;
    Optional<Order> cancelOrder(String orderId);
    List<Order> getOrdersForCustomer(String customerId);

    class OrderPlacementException extends Exception {
        public OrderPlacementException(String message) { super(message); }
    }

    class InvalidOrderStateException extends Exception {
        public InvalidOrderStateException(String message) { super(message); }
    }
}
