package elevator.model;

import java.util.HashSet;
import java.util.Set;

// One car. Owns its position, direction and pending stops, and advances
// itself one tick at a time via step() using SCAN: keep going in the
// current direction while there is anything ahead, then reverse.
public class Elevator {
    private final String id;
    private final int minFloor;
    private final int maxFloor;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private final Set<Request> requests = new HashSet<>();

    public Elevator(String id, int minFloor, int maxFloor, int startFloor) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = startFloor;
    }

    public boolean addRequest(Request request) {
        int floor = request.floor();
        if (floor < minFloor || floor > maxFloor) return false;
        // Already here and the doors would open for this request anyway
        if (floor == currentFloor && servesHere(request.type())) return true;
        requests.add(request);
        return true;
    }

    public void step() {
        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            return;
        }

        if (direction == Direction.IDLE) {
            direction = directionToward(nearestRequest());
        }

        // Stop: serve only what matches our travel direction. A stop
        // consumes the tick (doors open), so we don't also move.
        boolean stopped = requests.remove(new Request(currentFloor, RequestType.DESTINATION));
        stopped |= requests.remove(new Request(currentFloor, pickupTypeFor(direction)));
        if (stopped) return;

        // Nothing left ahead: reverse. The next tick re-checks this floor
        // for requests in the new direction before moving.
        if (!hasRequestsAhead()) {
            direction = direction == Direction.UP ? Direction.DOWN : Direction.UP;
            return;
        }

        currentFloor += direction == Direction.UP ? 1 : -1;
    }

    private boolean servesHere(RequestType type) {
        if (direction == Direction.IDLE) return requests.isEmpty();
        return type == RequestType.DESTINATION
                || type == pickupTypeFor(direction);
    }

    private boolean hasRequestsAhead() {
        for (Request r : requests) {
            if (direction == Direction.UP && r.floor() > currentFloor) return true;
            if (direction == Direction.DOWN && r.floor() < currentFloor) return true;
        }
        return false;
    }

    // Lowest floor wins ties so behavior doesn't depend on HashSet order
    private Request nearestRequest() {
        Request best = null;
        for (Request r : requests) {
            if (best == null) { best = r; continue; }
            int d = Math.abs(r.floor() - currentFloor);
            int bestD = Math.abs(best.floor() - currentFloor);
            if (d < bestD || (d == bestD && r.floor() < best.floor())) best = r;
        }
        return best;
    }

    private Direction directionToward(Request r) {
        if (r.floor() > currentFloor) return Direction.UP;
        if (r.floor() < currentFloor) return Direction.DOWN;
        return r.type() == RequestType.PICKUP_DOWN ? Direction.DOWN : Direction.UP;
    }

    private static RequestType pickupTypeFor(Direction direction) {
        return direction == Direction.DOWN ? RequestType.PICKUP_DOWN : RequestType.PICKUP_UP;
    }

    public String getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public Direction getDirection() { return direction; }
    public boolean isIdle() { return requests.isEmpty(); }

    @Override
    public String toString() {
        return id + "[floor=" + currentFloor + ", " + direction + ", stops=" + requests + "]";
    }
}
