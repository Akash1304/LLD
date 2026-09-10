package elevator.model;

public class HallCall {
    private final int floor;
    private final Direction direction;

    public HallCall(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    public int getFloor() { return floor; }
    public Direction getDirection() { return direction; }

    @Override
    public String toString() { return "HallCall{floor=" + floor + ", dir=" + direction + '}'; }
}
