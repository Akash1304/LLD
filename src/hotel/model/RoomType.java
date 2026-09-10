package hotel.model;

public enum RoomType {
    SINGLE(80.0),
    DOUBLE(120.0),
    SUITE(220.0);

    private final double baseNightlyRate;

    RoomType(double baseNightlyRate) { this.baseNightlyRate = baseNightlyRate; }

    public double getBaseNightlyRate() { return baseNightlyRate; }
}
