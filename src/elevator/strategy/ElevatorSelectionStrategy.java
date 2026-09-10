package elevator.strategy;

import elevator.model.Elevator;
import elevator.model.HallCall;

import java.util.List;

public interface ElevatorSelectionStrategy {
    Elevator selectElevator(List<Elevator> elevators, HallCall call);
}
