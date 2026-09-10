package elevator.service;

import elevator.command.ElevatorCommand;
import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.model.HallCall;
import elevator.strategy.ElevatorSelectionStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class InMemoryElevatorControlSystem implements ElevatorControlSystem {
    private final List<Elevator> elevators;
    private final Map<String, Elevator> byId;
    private final ElevatorSelectionStrategy selectionStrategy;
    // Producer/consumer boundary: any number of button threads offer()
    // commands; only the control loop (step) takes them. LinkedBlockingQueue
    // is internally locked, so submit() needs no synchronization of its
    // own and ordering is arrival order.
    private final BlockingQueue<ElevatorCommand> pending = new LinkedBlockingQueue<>();

    // The bank of elevators is fixed at construction (no addElevator/
    // removeElevator API), so the LIST itself needs no synchronization.
    // Each individual Elevator still guards its own mutable state
    // (floor/direction/stops) with its own monitor.
    public InMemoryElevatorControlSystem(List<Elevator> elevators, ElevatorSelectionStrategy selectionStrategy) {
        this.elevators = Collections.unmodifiableList(elevators);
        this.byId = elevators.stream().collect(Collectors.toMap(Elevator::getId, e -> e));
        this.selectionStrategy = selectionStrategy;
    }

    @Override
    public void submit(ElevatorCommand command) {
        pending.offer(command);
    }

    @Override
    public void step() {
        ElevatorCommand command;
        while ((command = pending.poll()) != null) {
            command.execute(this);
        }
        for (Elevator e : elevators) e.step();
    }

    @Override
    public Elevator dispatchHallCall(int floor, Direction direction) {
        // selectElevator() reads each Elevator through its synchronized
        // getters (a best-effort, not a locked-together snapshot), so the
        // elevator picked is a reasonable candidate at scoring time;
        // requestStop() is then atomic on that specific elevator. There's
        // no "double-booking" to race on -- a hall call just enqueues a
        // stop -- so no retry loop is needed, only per-elevator consistency.
        Elevator chosen = selectionStrategy.selectElevator(elevators, new HallCall(floor, direction));
        chosen.requestStop(floor);
        return chosen;
    }

    @Override
    public void dispatchCarCall(String elevatorId, int floor) {
        Elevator elevator = byId.get(elevatorId);
        if (elevator == null) throw new IllegalArgumentException("Unknown elevator: " + elevatorId);
        elevator.requestStop(floor);
    }

    @Override
    public List<Elevator> getElevators() {
        return elevators;
    }

    @Override
    public boolean allIdle() {
        return pending.isEmpty() && elevators.stream().allMatch(e -> e.getDirection() == Direction.IDLE);
    }
}
