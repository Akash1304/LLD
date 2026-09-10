package shoppingcart.strategy;

// A common cart-level promo shape: "$X off orders over $Y" -- only kicks
// in once the subtotal clears a threshold, unlike a percentage discount
// which always applies.
public class MinimumSpendFlatDiscountStrategy implements DiscountStrategy {
    private final double minimumSpend;
    private final double flatDiscount;

    public MinimumSpendFlatDiscountStrategy(double minimumSpend, double flatDiscount) {
        this.minimumSpend = minimumSpend;
        this.flatDiscount = flatDiscount;
    }

    @Override
    public double apply(double subtotal) {
        return subtotal >= minimumSpend ? Math.max(0, subtotal - flatDiscount) : subtotal;
    }
}
