package ecommerce.strategy;

public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public double apply(double subtotal) { return subtotal; }
}
