package elevator.strategy;

import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.model.HallCall;

import java.util.Comparator;
import java.util.List;

// Prefers an idle elevator closest to the call; among moving elevators,
// prefers one already heading toward the call in the same direction (so it
// can pick the passenger up "on the way") before considering others.
public class NearestElevatorStrategy implements ElevatorSelectionStrategy {
    @Override
    public Elevator selectElevator(List<Elevator> elevators, HallCall call) {
        return elevators.stream()
                .min(Comparator.comparingInt(e -> score(e, call)))
                .orElseThrow(() -> new IllegalStateException("No elevators available"));
    }

    private int score(Elevator e, HallCall call) {
        int distance = Math.abs(e.getCurrentFloor() - call.getFloor());
        boolean idle = e.getDirection() == Direction.IDLE;
        boolean movingTowardAndSameDirection = e.getDirection() == call.getDirection()
                && ((call.getDirection() == Direction.UP && e.getCurrentFloor() <= call.getFloor())
                || (call.getDirection() == Direction.DOWN && e.getCurrentFloor() >= call.getFloor()));

        if (idle) return distance;
        if (movingTowardAndSameDirection) return distance;
        return distance + 1000; // heavily deprioritize elevators moving away or in the wrong direction
    }
}
