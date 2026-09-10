package shoppingcart.service;

import shoppingcart.model.Receipt;
import shoppingcart.strategy.DiscountStrategy;

public interface CheckoutService {
    Receipt checkout(String customerId, DiscountStrategy discountStrategy) throws EmptyCartException;

    class EmptyCartException extends Exception {
        public EmptyCartException(String message) { super(message); }
    }
}
