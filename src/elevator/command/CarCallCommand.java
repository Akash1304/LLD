package elevator.command;

import elevator.service.ElevatorControlSystem;

public class CarCallCommand implements ElevatorCommand {
    private final String elevatorId;
    private final int floor;

    public CarCallCommand(String elevatorId, int floor) {
        this.elevatorId = elevatorId;
        this.floor = floor;
    }

    @Override
    public void execute(ElevatorControlSystem system) {
        system.dispatchCarCall(elevatorId, floor);
        System.out.println("  " + describe() + " -> queued on " + elevatorId);
    }

    @Override
    public String describe() { return "CarCall{" + elevatorId + " -> floor " + floor + "}"; }
}
