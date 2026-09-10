package ecommerce.strategy;

// Never charges at placement time -- payment is collected in person on
// delivery, so the order proceeds while remaining unpaid until then.
public class CashOnDeliveryPaymentStrategy implements PaymentStrategy {
    @Override
    public boolean isPayNow() { return false; }

    @Override
    public void pay(double amount) {
        System.out.println("  $" + amount + " will be collected on delivery");
    }

    @Override
    public String getName() { return "CASH_ON_DELIVERY"; }
}
