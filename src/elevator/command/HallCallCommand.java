package elevator.command;

import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.service.ElevatorControlSystem;

public class HallCallCommand implements ElevatorCommand {
    private final int floor;
    private final Direction direction;

    public HallCallCommand(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

    @Override
    public void execute(ElevatorControlSystem system) {
        Elevator chosen = system.dispatchHallCall(floor, direction);
        System.out.println("  " + describe() + " -> dispatched " + chosen.getId());
    }

    @Override
    public String describe() { return "HallCall{floor=" + floor + ", " + direction + "}"; }
}
