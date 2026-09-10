package fooddelivery.observer;

import fooddelivery.model.FoodOrder;
import fooddelivery.model.OrderStatus;

// Stand-in for the customer's app screen receiving a push on each change.
public class CustomerTrackingListener implements OrderStatusListener {
    @Override
    public void onStatusChanged(FoodOrder order, OrderStatus from, OrderStatus to) {
        String agent = order.getAssignedAgent() != null ? " (agent " + order.getAssignedAgent().getName() + ")" : "";
        System.out.println("  [" + order.getCustomerId() + "'s app] " + order.getId() + ": " + from + " -> " + to + agent);
    }
}
