package fooddelivery.observer;

import fooddelivery.model.FoodOrder;
import fooddelivery.model.OrderStatus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// The Observer "subject". OrderService publishes; the customer's tracking
// screen, the restaurant's kitchen display, and the agent's app can each
// subscribe without OrderService knowing any of them exist.
public class OrderEventPublisher {
    private final List<OrderStatusListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(OrderStatusListener listener) { listeners.add(listener); }
    public void unsubscribe(OrderStatusListener listener) { listeners.remove(listener); }

    public void publish(FoodOrder order, OrderStatus from, OrderStatus to) {
        for (OrderStatusListener listener : listeners) listener.onStatusChanged(order, from, to);
    }
}
