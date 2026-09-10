package ecommerce.strategy;

public class WalletPaymentStrategy implements PaymentStrategy {
    private double walletBalance;

    public WalletPaymentStrategy(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    @Override
    public boolean isPayNow() { return true; }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (amount > walletBalance) {
            throw new PaymentFailedException("Wallet balance $" + walletBalance + " is less than $" + amount);
        }
        walletBalance -= amount;
        System.out.println("  Deducted $" + amount + " from wallet, remaining balance $" + walletBalance);
    }

    @Override
    public String getName() { return "WALLET"; }
}
