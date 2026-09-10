package ridesharing.model;

public class Location {
    private final double x;
    private final double y;

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    // Euclidean distance on a flat plane -- a deliberate simplification of
    // real-world lat/lon distance (haversine); see the README trade-off
    // table for why that's fine at interview scope.
    public double distanceTo(Location other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() { return "(" + x + "," + y + ")"; }
}
