package fooddelivery.observer;

import fooddelivery.model.FoodOrder;
import fooddelivery.model.OrderStatus;

// Observer contract for "real-time order tracking": every status
// transition is pushed to subscribers the moment it commits, instead of
// the customer app polling getOrder() on a timer.
public interface OrderStatusListener {
    void onStatusChanged(FoodOrder order, OrderStatus from, OrderStatus to);
}
