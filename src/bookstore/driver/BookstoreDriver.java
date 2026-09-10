package bookstore.driver;

import bookstore.model.Book;
import bookstore.model.Customer;
import bookstore.model.Order;
import bookstore.service.CatalogService;
import bookstore.service.InMemoryCatalogService;
import bookstore.service.InMemoryInventoryService;
import bookstore.service.InMemoryOrderService;
import bookstore.service.InventoryService;
import bookstore.service.OrderService;
import bookstore.strategy.BulkDiscountPricingStrategy;

import static bookstore.specification.BookSpecifications.authorContains;
import static bookstore.specification.BookSpecifications.priceBelow;
import static bookstore.specification.BookSpecifications.subjectContains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BookstoreDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        CatalogService catalogService = new InMemoryCatalogService();
        InventoryService inventoryService = new InMemoryInventoryService(catalogService);
        OrderService orderService = new InMemoryOrderService(catalogService, inventoryService, new BulkDiscountPricingStrategy());

        catalogService.addBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 45.0, 10));
        catalogService.addBook(new Book("978-0132350884", "Clean Code", "Robert Martin", "Programming", 40.0, 3));
        catalogService.addBook(new Book("978-0201633610", "Design Patterns", "Erich Gamma", "Programming", 55.0, 5));

        System.out.println("Searching for author 'Robert Martin':");
        List<Book> results = catalogService.search(authorContains("Robert Martin"));
        results.forEach(System.out::println);

        // Specification: leaf predicates compose -- no new class needed for
        // a two-field query
        System.out.println("\nSearching for 'Programming' books under $50:");
        catalogService.search(subjectContains("Programming").and(priceBelow(50))).forEach(System.out::println);

        Customer alice = new Customer("C1", "Alice", "alice@example.com");

        System.out.println("\nPlacing order: 2x Effective Java, 3x Clean Code (bulk discount should apply, qty >= 5):");
        Map<String, Integer> cart = new HashMap<>();
        cart.put("978-0134685991", 2);
        cart.put("978-0132350884", 3);
        try {
            Order order = orderService.placeOrder(alice, cart);
            System.out.println("Order placed: " + order);
            order.getItems().forEach(System.out::println);
        } catch (OrderService.InsufficientStockException e) {
            System.out.println("Order failed: " + e.getMessage());
        }

        System.out.println("\nAttempting to over-order Clean Code (only " + inventoryService.getStock("978-0132350884") + " left):");
        Map<String, Integer> overCart = new HashMap<>();
        overCart.put("978-0132350884", 10);
        try {
            orderService.placeOrder(alice, overCart);
        } catch (OrderService.InsufficientStockException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        System.out.println("\nCancelling first order and restoring stock:");
        List<Order> aliceOrders = orderService.getOrdersForCustomer("C1");
        Optional<Order> cancelled = orderService.cancelOrder(aliceOrders.get(0).getId());
        cancelled.ifPresent(o -> System.out.println("Cancelled: " + o));
        System.out.println("Stock for Clean Code after cancellation: " + inventoryService.getStock("978-0132350884"));
    }
}
