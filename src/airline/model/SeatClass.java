package airline.model;

public enum SeatClass {
    ECONOMY(1.0),
    BUSINESS(2.5),
    FIRST(4.0);

    private final double priceMultiplier;

    SeatClass(double priceMultiplier) { this.priceMultiplier = priceMultiplier; }

    public double getPriceMultiplier() { return priceMultiplier; }
}
