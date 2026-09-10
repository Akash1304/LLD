package shoppingcart.driver;

import shoppingcart.model.Cart;
import shoppingcart.model.Product;
import shoppingcart.model.Receipt;
import shoppingcart.service.CartService;
import shoppingcart.service.CheckoutService;
import shoppingcart.service.InMemoryCartService;
import shoppingcart.service.InMemoryCheckoutService;
import shoppingcart.strategy.MinimumSpendFlatDiscountStrategy;
import shoppingcart.strategy.NoDiscountStrategy;

public class ShoppingCartDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        CartService cartService = new InMemoryCartService();
        CheckoutService checkoutService = new InMemoryCheckoutService(cartService);

        Product mouse = new Product("P1", "Wireless Mouse", 25.0);
        Product keyboard = new Product("P2", "Mechanical Keyboard", 80.0);
        Product mousepad = new Product("P3", "Mouse Pad", 10.0);

        System.out.println("Alice adds items to her cart:");
        cartService.addItem("alice", mouse, 2);
        cartService.addItem("alice", keyboard, 1);
        cartService.addItem("alice", mousepad, 1);
        System.out.println(cartService.getOrCreateCart("alice"));

        System.out.println("\nAlice changes her mind: only 1 mouse, and removes the mouse pad:");
        cartService.updateQuantity("alice", "P1", 1);
        cartService.removeItem("alice", "P3");
        System.out.println(cartService.getOrCreateCart("alice"));

        try {
            System.out.println("\nChecking out with a '$15 off orders over $50' promo:");
            Receipt receipt = checkoutService.checkout("alice", new MinimumSpendFlatDiscountStrategy(50.0, 15.0));
            System.out.println(receipt);
            System.out.println("Items on receipt: " + receipt.getItems());

            System.out.println("\nAlice's cart is now empty: " + cartService.getOrCreateCart("alice"));

            System.out.println("\nTrying to check out again with an empty cart:");
            try {
                checkoutService.checkout("alice", new NoDiscountStrategy());
            } catch (CheckoutService.EmptyCartException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nBob adds one item under the $50 promo threshold (no discount applies):");
            cartService.addItem("bob", mousepad, 1);
            Receipt bobReceipt = checkoutService.checkout("bob", new MinimumSpendFlatDiscountStrategy(50.0, 15.0));
            System.out.println(bobReceipt);
        } catch (CheckoutService.EmptyCartException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
