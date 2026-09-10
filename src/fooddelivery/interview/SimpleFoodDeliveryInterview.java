package fooddelivery.interview;

import java.util.*;

// Compact, single-file interview-friendly food delivery demo.
// Supports: placing an order, nearest-agent assignment, and completing
// a delivery which frees the agent for the next order.
public class SimpleFoodDeliveryInterview {

    static class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        double distanceTo(Point o) { return Math.sqrt(Math.pow(x - o.x, 2) + Math.pow(y - o.y, 2)); }
    }

    static class Agent {
        final String id;
        Point location;
        boolean available = true;
        Agent(String id, Point location) { this.id = id; this.location = location; }
    }

    enum Status { PLACED, OUT_FOR_DELIVERY, DELIVERED }

    static class Order {
        final String id;
        final String customer;
        Agent agent;
        Status status = Status.PLACED;
        Order(String id, String customer) { this.id = id; this.customer = customer; }
    }

    final List<Agent> agents = new ArrayList<>();
    final Map<String, Order> orders = new HashMap<>();
    int counter = 1;

    void addAgent(Agent a) { agents.add(a); }

    Order placeOrder(String customer) {
        Order order = new Order("ORD-" + counter++, customer);
        orders.put(order.id, order);
        return order;
    }

    void assignDelivery(String orderId, Point restaurantLocation) throws Exception {
        Order order = orders.get(orderId);
        Agent nearest = agents.stream()
                .filter(a -> a.available)
                .min(Comparator.comparingDouble(a -> a.location.distanceTo(restaurantLocation)))
                .orElseThrow(() -> new Exception("No available delivery agent"));
        nearest.available = false;
        order.agent = nearest;
        order.status = Status.OUT_FOR_DELIVERY;
    }

    void completeDelivery(String orderId) {
        Order order = orders.get(orderId);
        order.agent.available = true;
        order.status = Status.DELIVERED;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleFoodDeliveryInterview system = new SimpleFoodDeliveryInterview();
        System.out.println("== Simple Food Delivery Interview Demo ==");

        system.addAgent(new Agent("A1", new Point(1, 1)));
        system.addAgent(new Agent("A2", new Point(5, 5)));

        Point restaurant = new Point(0, 0);

        try {
            Order order1 = system.placeOrder("alice");
            system.assignDelivery(order1.id, restaurant);
            System.out.println("Alice's order assigned to " + order1.agent.id);

            Order order2 = system.placeOrder("bob");
            system.assignDelivery(order2.id, restaurant);
            System.out.println("Bob's order assigned to " + order2.agent.id);

            System.out.println("\nBoth agents busy -- a third order fails:");
            Order order3 = system.placeOrder("carol");
            try {
                system.assignDelivery(order3.id, restaurant);
            } catch (Exception e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nAlice's order is delivered, freeing " + order1.agent.id + ":");
            system.completeDelivery(order1.id);

            System.out.println("Carol's order can now be assigned:");
            system.assignDelivery(order3.id, restaurant);
            System.out.println("Carol's order assigned to " + order3.agent.id);
        } catch (Exception e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
