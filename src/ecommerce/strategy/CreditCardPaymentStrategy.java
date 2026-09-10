package ecommerce.strategy;

public class CreditCardPaymentStrategy implements PaymentStrategy {
    private final String cardNumber;

    public CreditCardPaymentStrategy(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public boolean isPayNow() { return true; }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new PaymentFailedException("Invalid card number");
        }
        System.out.println("  Charged $" + amount + " to card ending in " + cardNumber.substring(12));
    }

    @Override
    public String getName() { return "CREDIT_CARD"; }
}
