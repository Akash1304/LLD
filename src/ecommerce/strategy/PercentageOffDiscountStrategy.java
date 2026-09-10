package ecommerce.strategy;

public class PercentageOffDiscountStrategy implements DiscountStrategy {
    private final double percentageOff;

    public PercentageOffDiscountStrategy(double percentageOff) {
        this.percentageOff = percentageOff;
    }

    @Override
    public double apply(double subtotal) {
        return subtotal * (1 - percentageOff / 100.0);
    }
}
