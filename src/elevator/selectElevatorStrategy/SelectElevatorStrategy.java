package elevator.selectElevatorStrategy;

import elevator.model.Elevator;
import elevator.model.RequestType;

import java.util.List;
import java.util.Optional;

public interface SelectElevatorStrategy {
    Optional<Elevator> selectElevator(int floor, RequestType type, List<Elevator> elevators);
}
