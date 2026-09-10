package ridesharing.strategy;

import ridesharing.model.Location;

// Wraps another FareStrategy and multiplies its result by a surge factor --
// a Decorator-shaped composition rather than a from-scratch reimplementation
// of the base fare math, so surge pricing can layer on top of *any* base
// strategy (e.g. standard, or a future promo-discount strategy).
public class SurgePricingFareStrategy implements FareStrategy {
    private final FareStrategy baseStrategy;
    private final double surgeMultiplier;

    public SurgePricingFareStrategy(FareStrategy baseStrategy, double surgeMultiplier) {
        this.baseStrategy = baseStrategy;
        this.surgeMultiplier = surgeMultiplier;
    }

    @Override
    public double calculateFare(Location pickup, Location dropoff) {
        return baseStrategy.calculateFare(pickup, dropoff) * surgeMultiplier;
    }
}
