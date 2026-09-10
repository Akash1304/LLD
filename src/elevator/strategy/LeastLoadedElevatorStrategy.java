package elevator.strategy;

import elevator.model.Elevator;
import elevator.model.HallCall;

import java.util.Comparator;
import java.util.List;

// Picks whichever elevator has the fewest pending stops, regardless of
// distance -- trades a possibly-longer wait for this one call in exchange
// for spreading load evenly across the bank of elevators.
public class LeastLoadedElevatorStrategy implements ElevatorSelectionStrategy {
    @Override
    public Elevator selectElevator(List<Elevator> elevators, HallCall call) {
        return elevators.stream()
                .min(Comparator.comparingInt(Elevator::pendingStopCount)
                        .thenComparingInt(e -> Math.abs(e.getCurrentFloor() - call.getFloor())))
                .orElseThrow(() -> new IllegalStateException("No elevators available"));
    }
}
