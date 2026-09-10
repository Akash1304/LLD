package moviebooking.model;

public enum SeatType {
    REGULAR(1.0),
    PREMIUM(1.5);

    private final double priceMultiplier;

    SeatType(double priceMultiplier) { this.priceMultiplier = priceMultiplier; }

    public double getPriceMultiplier() { return priceMultiplier; }
}
