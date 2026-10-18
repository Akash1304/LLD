package elevator.controller;

import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.model.Request;
import elevator.model.RequestType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

// Orchestrator: owns the bank, dispatches hall calls to one elevator, and
// advances the whole simulation one tick per step(). Destinations are not
// routed through here - a passenger presses them on the car's own panel.
public class ElevatorController {
    public static final int NUM_ELEVATORS = 3;
    public static final int MIN_FLOOR = 0;
    public static final int MAX_FLOOR = 9;

    private final List<Elevator> elevators = new ArrayList<>();

    public ElevatorController() {
        for (int i = 1; i <= NUM_ELEVATORS; i++) {
            elevators.add(new Elevator("E" + i, MIN_FLOOR, MAX_FLOOR, MIN_FLOOR));
        }
    }

    public boolean requestElevator(int floor, RequestType type) {
        if (floor < MIN_FLOOR || floor > MAX_FLOOR) return false;
        if (type == RequestType.DESTINATION) return false;

        Elevator best = selectBestElevator(floor, type);
        return best.addRequest(new Request(floor, type));
    }

    public void step() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
    }

    // Priority tiers:
    //   1. already moving toward the floor in the caller's direction (picks
    //      them up on the way, no extra reversal)
    //   2. nearest idle elevator
    //   3. nearest elevator of any kind (it'll get there after its sweep)
    private Elevator selectBestElevator(int floor, RequestType type) {
        Direction wanted = type == RequestType.PICKUP_UP ? Direction.UP : Direction.DOWN;

        Elevator best = nearest(floor, e -> e.getDirection() == wanted && isMovingToward(e, floor));
        if (best == null) best = nearest(floor, Elevator::isIdle);
        if (best == null) best = nearest(floor, e -> true);
        return best;
    }

    private boolean isMovingToward(Elevator e, int floor) {
        if (e.getDirection() == Direction.UP) return e.getCurrentFloor() <= floor;
        if (e.getDirection() == Direction.DOWN) return e.getCurrentFloor() >= floor;
        return false;
    }

    private Elevator nearest(int floor, Predicate<Elevator> filter) {
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

    public Elevator getElevator(int index) { return elevators.get(index); }
    public List<Elevator> getElevators() { return Collections.unmodifiableList(elevators); }

    public boolean allIdle() {
        for (Elevator e : elevators) {
            if (!e.isIdle()) return false;
        }
        return true;
    }
}
