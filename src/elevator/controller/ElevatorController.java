package elevator.controller;

import elevator.model.Elevator;
import elevator.model.Request;
import elevator.model.RequestType;
import elevator.selectElevatorStrategy.SelectElevatorStrategy;
import elevator.selectElevatorStrategy.SelectNearest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Orchestrator: owns the bank, dispatches hall calls to one elevator, and
// advances the whole simulation one tick per step(). Destinations are not
// routed through here - a passenger presses them on the car's own panel.
public class ElevatorController {
    public static final int NUM_ELEVATORS = 3;
    public static final int MIN_FLOOR = 0;
    public static final int MAX_FLOOR = 9;

    private final List<Elevator> elevators = new ArrayList<>();
    private final SelectElevatorStrategy selectionStrategy;


    public ElevatorController(SelectElevatorStrategy selectionStrategy) {
        this.selectionStrategy = selectionStrategy;
        for (int i = 1; i <= NUM_ELEVATORS; i++) {
            elevators.add(new Elevator("E" + i, MIN_FLOOR, MAX_FLOOR, MIN_FLOOR));
        }
    }

    public boolean requestElevator(int floor, RequestType type) {
        if (floor < MIN_FLOOR || floor > MAX_FLOOR) return false;
        if (type == RequestType.DESTINATION) return false;

        return selectionStrategy.selectElevator(floor, type, elevators)
                .map(e -> e.addRequest(new Request(floor, type)))
                .orElse(false);
    }

    public void step() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
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
