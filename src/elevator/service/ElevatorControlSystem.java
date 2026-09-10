package elevator.service;

import elevator.command.ElevatorCommand;
import elevator.model.Direction;
import elevator.model.Elevator;

import java.util.List;

public interface ElevatorControlSystem {
    // producers (buttons) call this from any thread; it never blocks on
    // elevator state, it just enqueues
    void submit(ElevatorCommand command);

    // the control loop: drain queued commands in arrival order, then
    // advance every elevator one floor
    void step();

    // primitives that commands call during execute() -- not for buttons
    Elevator dispatchHallCall(int floor, Direction direction);
    void dispatchCarCall(String elevatorId, int floor);

    List<Elevator> getElevators();
    boolean allIdle();
}
