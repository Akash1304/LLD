package fooddelivery.driver;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.FoodOrder;
import fooddelivery.model.Location;
import fooddelivery.model.MenuItem;
import fooddelivery.model.Restaurant;
import fooddelivery.observer.CustomerTrackingListener;
import fooddelivery.observer.OrderEventPublisher;
import fooddelivery.service.DeliveryAgentRegistry;
import fooddelivery.service.InMemoryDeliveryAgentRegistry;
import fooddelivery.service.InMemoryOrderService;
import fooddelivery.service.InMemoryRestaurantService;
import fooddelivery.service.OrderService;
import fooddelivery.service.RestaurantService;
import fooddelivery.strategy.LeastBusyAgentStrategy;
import fooddelivery.strategy.NearestAgentStrategy;

import java.util.HashMap;
import java.util.Map;

public class FoodDeliveryDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        RestaurantService restaurantService = new InMemoryRestaurantService();
        DeliveryAgentRegistry agentRegistry = new InMemoryDeliveryAgentRegistry();
        // Observer: every status change is pushed to the customer's app
        OrderEventPublisher publisher = new OrderEventPublisher();
        publisher.subscribe(new CustomerTrackingListener());
        OrderService orderService = new InMemoryOrderService(restaurantService, agentRegistry, publisher);

        Restaurant pizzaPlace = restaurantService.addRestaurant(new Restaurant("R1", "Pizza Place", new Location(0, 0)));
        pizzaPlace.addMenuItem(new MenuItem("M1", "Margherita Pizza", 12.0));
        pizzaPlace.addMenuItem(new MenuItem("M2", "Garlic Bread", 5.0));

        agentRegistry.registerAgent(new DeliveryAgent("A1", "Sam", new Location(1, 1)));
        agentRegistry.registerAgent(new DeliveryAgent("A2", "Nina", new Location(5, 5)));

        try {
            System.out.println("Alice orders a pizza and garlic bread:");
            Map<String, Integer> cart = new HashMap<>();
            cart.put("M1", 1);
            cart.put("M2", 1);
            FoodOrder order = orderService.placeOrder("alice", "R1", new Location(2, 2), cart);
            System.out.println(order);

            System.out.println("\nRestaurant starts preparing:");
            orderService.markPreparing(order.getId());
            System.out.println(order);

            System.out.println("\nAssigning delivery via nearest-agent strategy (Sam is closer to the restaurant):");
            orderService.assignDelivery(order.getId(), new NearestAgentStrategy());
            System.out.println(order);

            System.out.println("\nBob orders while Sam is busy -- Nina should be picked (only agent left):");
            Map<String, Integer> bobCart = new HashMap<>();
            bobCart.put("M1", 2);
            FoodOrder bobOrder = orderService.placeOrder("bob", "R1", new Location(6, 6), bobCart);
            orderService.markPreparing(bobOrder.getId());
            orderService.assignDelivery(bobOrder.getId(), new LeastBusyAgentStrategy());
            System.out.println(bobOrder);

            System.out.println("\nBoth agents busy -- a third order fails to find a delivery agent:");
            Map<String, Integer> carolCart = new HashMap<>();
            carolCart.put("M2", 3);
            FoodOrder carolOrder = orderService.placeOrder("carol", "R1", new Location(3, 3), carolCart);
            orderService.markPreparing(carolOrder.getId());
            try {
                orderService.assignDelivery(carolOrder.getId(), new NearestAgentStrategy());
            } catch (OrderService.OrderException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nSam completes the delivery, becomes available again:");
            orderService.completeDelivery(order.getId());
            System.out.println(order);

            System.out.println("\nNow Carol's order can be assigned to Sam:");
            orderService.assignDelivery(carolOrder.getId(), new NearestAgentStrategy());
            System.out.println(carolOrder);
        } catch (OrderService.OrderException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
