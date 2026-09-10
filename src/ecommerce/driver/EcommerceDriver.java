package ecommerce.driver;

import ecommerce.model.Address;
import ecommerce.model.Order;
import ecommerce.model.Product;
import ecommerce.service.InMemoryOrderService;
import ecommerce.service.InMemoryProductCatalogService;
import ecommerce.service.OrderService;
import ecommerce.service.ProductCatalogService;
import ecommerce.strategy.CashOnDeliveryPaymentStrategy;
import ecommerce.strategy.CreditCardPaymentStrategy;
import ecommerce.strategy.NoDiscountStrategy;
import ecommerce.strategy.PercentageOffDiscountStrategy;
import ecommerce.strategy.WalletPaymentStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        ProductCatalogService catalog = new InMemoryProductCatalogService();
        OrderService orderService = new InMemoryOrderService(catalog);

        catalog.addProduct(new Product("P1", "Wireless Mouse", "Electronics", 25.0, 10));
        catalog.addProduct(new Product("P2", "Mechanical Keyboard", "Electronics", 80.0, 3));

        Address address = new Address("123 Main St", "Springfield", "12345");

        System.out.println("Electronics catalog: " + catalog.searchByCategory("Electronics"));

        try {
            System.out.println("\nAlice orders 1 mouse + 1 keyboard, paying by credit card, with a 10% off coupon:");
            Map<String, Integer> cart = new HashMap<>();
            cart.put("P1", 1);
            cart.put("P2", 1);
            Order aliceOrder = orderService.placeOrder("alice", cart, address,
                    new CreditCardPaymentStrategy("1234567812345678"), new PercentageOffDiscountStrategy(10));
            System.out.println(aliceOrder);

            System.out.println("\nBob orders 5 keyboards but only 2 are left (should fail, nothing charged):");
            try {
                Map<String, Integer> bobCart = new HashMap<>();
                bobCart.put("P2", 5);
                orderService.placeOrder("bob", bobCart, address, new WalletPaymentStrategy(500.0), new NoDiscountStrategy());
            } catch (OrderService.OrderPlacementException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nBob orders 1 keyboard cash-on-delivery (stays PLACED, not PAID, until delivery):");
            Map<String, Integer> bobCart2 = new HashMap<>();
            bobCart2.put("P2", 1);
            Order bobOrder = orderService.placeOrder("bob", bobCart2, address, new CashOnDeliveryPaymentStrategy(), new NoDiscountStrategy());
            System.out.println(bobOrder);

            System.out.println("\nShipping and delivering Alice's order:");
            orderService.shipOrder(aliceOrder.getId());
            Order delivered = orderService.deliverOrder(aliceOrder.getId());
            System.out.println(delivered);

            System.out.println("\nTrying to ship Bob's order before it's PAID (should fail -- it's COD, still PLACED):");
            try {
                orderService.shipOrder(bobOrder.getId());
            } catch (OrderService.InvalidOrderStateException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nCancelling Bob's COD order restores stock:");
            orderService.cancelOrder(bobOrder.getId()).ifPresent(o -> System.out.println("Cancelled: " + o));
            System.out.println("Keyboard stock now: " + catalog.getProduct("P2").get().getStock());

            System.out.println("\nAlice's order history: " + orderService.getOrdersForCustomer("alice"));
        } catch (OrderService.OrderPlacementException | OrderService.InvalidOrderStateException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
