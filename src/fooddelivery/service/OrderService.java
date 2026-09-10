package fooddelivery.service;

import fooddelivery.model.FoodOrder;
import fooddelivery.model.Location;
import fooddelivery.strategy.DeliveryAssignmentStrategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    FoodOrder placeOrder(String customerId, String restaurantId, Location deliveryLocation, Map<String, Integer> menuItemQuantities) throws OrderException;
    FoodOrder markPreparing(String orderId) throws OrderException;
    FoodOrder assignDelivery(String orderId, DeliveryAssignmentStrategy assignmentStrategy) throws OrderException;
    FoodOrder completeDelivery(String orderId) throws OrderException;
    Optional<FoodOrder> cancelOrder(String orderId);
    List<FoodOrder> getOrdersForCustomer(String customerId);

    class OrderException extends Exception {
        public OrderException(String message) { super(message); }
    }
}
