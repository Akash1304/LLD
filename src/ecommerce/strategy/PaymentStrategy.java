package ecommerce.strategy;

public interface PaymentStrategy {
    // whether this method charges at order placement (true) or defers
    // payment to a later event, e.g. delivery (false)
    boolean isPayNow();

    void pay(double amount) throws PaymentFailedException;

    String getName();

    class PaymentFailedException extends Exception {
        public PaymentFailedException(String message) { super(message); }
    }
}
