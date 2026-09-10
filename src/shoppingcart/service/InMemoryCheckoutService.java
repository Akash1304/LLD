package shoppingcart.service;

import shoppingcart.model.Cart;
import shoppingcart.model.Receipt;
import shoppingcart.strategy.DiscountStrategy;

import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCheckoutService implements CheckoutService {
    private final CartService cartService;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryCheckoutService(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public Receipt checkout(String customerId, DiscountStrategy discountStrategy) throws EmptyCartException {
        Cart cart = cartService.getOrCreateCart(customerId);

        // Cart's individual methods (isEmpty/getSubtotal/getItems/clear)
        // are each independently synchronized on the cart, but checkout
        // needs all four to act on the SAME, unchanging snapshot of the
        // cart's contents -- without this outer lock, a concurrent
        // addItem between getSubtotal() and clear() could either get
        // silently wiped by clear() with no receipt ever reflecting it,
        // or the receipt's subtotal could disagree with its own item
        // list. Synchronizing on the cart here composes safely with its
        // own synchronized methods (reentrant monitor).
        synchronized (cart) {
            if (cart.isEmpty()) {
                throw new EmptyCartException("Cannot check out an empty cart for " + customerId);
            }

            double subtotal = cart.getSubtotal();
            double total = discountStrategy.apply(subtotal);
            Receipt receipt = new Receipt("RCPT-" + idCounter.getAndIncrement(), customerId, cart.getItems(), subtotal, total);

            cart.clear();
            return receipt;
        }
    }
}
