package elevator.selectElevatorStrategy;

import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.model.RequestType;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// Priority tiers:
//   1. already moving toward the floor in the caller's direction (picks
//      them up on the way, no extra reversal)
//   2. nearest idle elevator
//   3. nearest elevator of any kind (it'll get there after its sweep)
public class SelectNearest implements SelectElevatorStrategy {
    @Override
    public Optional<Elevator> selectElevator(int floor, RequestType type, List<Elevator> elevators) {
        Direction wanted = type == RequestType.PICKUP_UP ? Direction.UP : Direction.DOWN;

        Elevator best = nearest(floor, elevators, e -> e.getDirection() == wanted && isMovingToward(e, floor));
        if (best == null) best = nearest(floor, elevators, Elevator::isIdle);
        if (best == null) best = nearest(floor, elevators, e -> true);
        return Optional.ofNullable(best);
    }

    private Elevator nearest(int floor, List<Elevator> elevators, Predicate<Elevator> filter) {
        Elevator best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            if (!filter.test(e)) continue;
            int distance = Math.abs(e.getCurrentFloor() - floor);
            if (distance < bestDistance) {
                best = e;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isMovingToward(Elevator e, int floor) {
        if (e.getDirection() == Direction.UP) return e.getCurrentFloor() <= floor;
        if (e.getDirection() == Direction.DOWN) return e.getCurrentFloor() >= floor;
        return false;
    }
}
