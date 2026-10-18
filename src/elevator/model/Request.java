package elevator.model;

// Value object: equality on (floor, type) so the elevator's Set dedupes
// repeated button presses for the same stop.
public record Request(int floor, RequestType type) {
    @Override
    public String toString() { return type + "@" + floor; }
}
