package fooddelivery.service;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.FoodOrder;
import fooddelivery.model.FoodOrderItem;
import fooddelivery.model.Location;
import fooddelivery.model.MenuItem;
import fooddelivery.model.OrderStatus;
import fooddelivery.model.Restaurant;
import fooddelivery.observer.OrderEventPublisher;
import fooddelivery.strategy.DeliveryAssignmentStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryOrderService implements OrderService {
    private final Map<String, FoodOrder> orders = new ConcurrentHashMap<>();
    private final RestaurantService restaurantService;
    private final DeliveryAgentRegistry agentRegistry;
    private final OrderEventPublisher publisher;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryOrderService(RestaurantService restaurantService, DeliveryAgentRegistry agentRegistry, OrderEventPublisher publisher) {
        this.restaurantService = restaurantService;
        this.agentRegistry = agentRegistry;
        this.publisher = publisher;
    }

    @Override
    public FoodOrder placeOrder(String customerId, String restaurantId, Location deliveryLocation, Map<String, Integer> menuItemQuantities) throws OrderException {
        Restaurant restaurant = restaurantService.getRestaurant(restaurantId)
                .orElseThrow(() -> new OrderException("Unknown restaurant: " + restaurantId));

        List<FoodOrderItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : menuItemQuantities.entrySet()) {
            MenuItem menuItem = restaurant.getMenuItem(entry.getKey())
                    .orElseThrow(() -> new OrderException("Unknown menu item: " + entry.getKey()));
            items.add(new FoodOrderItem(menuItem, entry.getValue()));
        }
        double total = items.stream().mapToDouble(FoodOrderItem::getLineTotal).sum();

        FoodOrder order = FoodOrder.builder()
                .id("ORD-" + idCounter.getAndIncrement())
                .customerId(customerId).restaurant(restaurant)
                .deliveryLocation(deliveryLocation).items(items).totalPrice(total)
                .build();
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public FoodOrder markPreparing(String orderId) throws OrderException {
        FoodOrder order = requireOrder(orderId);
        if (!order.tryTransition(OrderStatus.PLACED, OrderStatus.PREPARING)) {
            throw new OrderException("Order " + order.getId() + " must be " + OrderStatus.PLACED + " but is " + order.getStatus());
        }
        // publish only after the CAS succeeded: exactly one thread wins the
        // transition, so exactly one notification goes out per change
        publisher.publish(order, OrderStatus.PLACED, OrderStatus.PREPARING);
        return order;
    }

    // assignmentStrategy.selectAgent() is a lock-free read-only scan, so
    // its result is only a candidate; DeliveryAgent.tryClaim() is the
    // atomic primitive that actually claims one. No retry loop here (unlike
    // ParkingLot/RideSharing) because a food order isn't itself a scarce
    // resource being raced over -- if this specific agent was just taken,
    // the caller can simply retry assignDelivery with a fresh call, which
    // re-scans naturally.
    @Override
    public FoodOrder assignDelivery(String orderId, DeliveryAssignmentStrategy assignmentStrategy) throws OrderException {
        FoodOrder order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new OrderException("Order " + order.getId() + " must be " + OrderStatus.PREPARING + " but is " + order.getStatus());
        }

        DeliveryAgent agent = assignmentStrategy.selectAgent(agentRegistry.getAvailableAgents(), order.getRestaurant().getLocation())
                .orElseThrow(() -> new OrderException("No available delivery agent for order " + orderId));

        if (!agent.tryClaim()) {
            throw new OrderException("Agent " + agent.getId() + " was just claimed by another assignment, please retry");
        }
        if (!order.tryAssignAndTransition(agent, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY)) {
            // order's status changed (e.g. cancelled) between our check and
            // the claim -- release the agent we just took
            agent.setAvailable(true);
            throw new OrderException("Order " + orderId + " is no longer PREPARING, please retry");
        }
        publisher.publish(order, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY);
        return order;
    }

    @Override
    public FoodOrder completeDelivery(String orderId) throws OrderException {
        FoodOrder order = requireOrder(orderId);
        DeliveryAgent agent = order.getAssignedAgent();
        if (!order.tryTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)) {
            throw new OrderException("Order " + order.getId() + " must be " + OrderStatus.OUT_FOR_DELIVERY + " but is " + order.getStatus());
        }

        agent.setLocation(order.getDeliveryLocation());
        agent.incrementCompletedDeliveries();
        agent.setAvailable(true);
        publisher.publish(order, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
        return order;
    }

    @Override
    public Optional<FoodOrder> cancelOrder(String orderId) {
        FoodOrder order = orders.get(orderId);
        if (order == null) return Optional.empty();
        OrderStatus from;
        if (order.tryTransition(OrderStatus.PLACED, OrderStatus.CANCELLED)) {
            from = OrderStatus.PLACED;
        } else if (order.tryTransition(OrderStatus.PREPARING, OrderStatus.CANCELLED)) {
            from = OrderStatus.PREPARING;
        } else {
            return Optional.empty();
        }
        publisher.publish(order, from, OrderStatus.CANCELLED);
        return Optional.of(order);
    }

    @Override
    public List<FoodOrder> getOrdersForCustomer(String customerId) {
        List<FoodOrder> result = new ArrayList<>();
        for (FoodOrder o : orders.values()) {
            if (o.getCustomerId().equals(customerId)) result.add(o);
        }
        return result;
    }

    private FoodOrder requireOrder(String orderId) throws OrderException {
        FoodOrder order = orders.get(orderId);
        if (order == null) throw new OrderException("Unknown order: " + orderId);
        return order;
    }
}
