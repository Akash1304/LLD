package elevator.model;

import java.util.TreeSet;

// Runs the classic SCAN ("elevator algorithm"): keeps moving in its current
// direction, picking up every pending stop along the way, and only reverses
// once there are no more requests ahead of it in that direction.
public class Elevator {
    private final String id;
    private final int minFloor;
    private final int maxFloor;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>();

    public Elevator(String id, int minFloor, int maxFloor, int startFloor) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = startFloor;
    }

    public String getId() { return id; }
    public synchronized int getCurrentFloor() { return currentFloor; }
    public synchronized Direction getDirection() { return direction; }
    public synchronized int pendingStopCount() { return upStops.size() + downStops.size(); }

    // synchronized: requestStop() (called from hall/car-call dispatch,
    // potentially from multiple threads submitting requests concurrently)
    // and step() (called from the simulation/control loop) both read and
    // mutate currentFloor/direction/upStops/downStops together, and those
    // fields must change as one consistent unit -- e.g. "add a stop" and
    // "is now IDLE and should pick an initial direction" can't be split.
    // One monitor per Elevator: dispatching to elevator A never blocks a
    // concurrent dispatch to elevator B.
    public synchronized void requestStop(int floor) {
        if (floor < minFloor || floor > maxFloor) {
            throw new IllegalArgumentException("Floor " + floor + " is outside range [" + minFloor + "," + maxFloor + "]");
        }
        if (floor == currentFloor) return;
        if (floor > currentFloor) upStops.add(floor);
        else downStops.add(floor);

        if (direction == Direction.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }
    }

    // advances the elevator by one floor (or opens doors if already at a
    // requested stop); returns true if a stop was serviced this step
    public synchronized boolean step() {
        if (direction == Direction.IDLE) return false;

        if (direction == Direction.UP) {
            if (currentFloor < maxFloor) currentFloor++;
            if (upStops.remove(currentFloor)) {
                if (upStops.isEmpty()) {
                    direction = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN;
                }
                return true;
            }
        } else {
            if (currentFloor > minFloor) currentFloor--;
            if (downStops.remove(currentFloor)) {
                if (downStops.isEmpty()) {
                    direction = upStops.isEmpty() ? Direction.IDLE : Direction.UP;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized String toString() {
        return id + "[floor=" + currentFloor + ", dir=" + direction + ", up=" + upStops + ", down=" + downStops + ']';
    }
}
