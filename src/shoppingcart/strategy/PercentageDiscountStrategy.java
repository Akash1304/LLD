package shoppingcart.strategy;

public class PercentageDiscountStrategy implements DiscountStrategy {
    private final double percentageOff;

    public PercentageDiscountStrategy(double percentageOff) {
        this.percentageOff = percentageOff;
    }

    @Override
    public double apply(double subtotal) {
        return subtotal * (1 - percentageOff / 100.0);
    }
}
