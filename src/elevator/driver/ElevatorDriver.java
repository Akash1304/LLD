package elevator.driver;

import elevator.command.CarCallCommand;
import elevator.command.HallCallCommand;
import elevator.model.Direction;
import elevator.model.Elevator;
import elevator.service.ElevatorControlSystem;
import elevator.service.InMemoryElevatorControlSystem;
import elevator.strategy.NearestElevatorStrategy;

import java.util.Arrays;
import java.util.List;

public class ElevatorDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        List<Elevator> elevators = Arrays.asList(
                new Elevator("E1", 1, 10, 1),
                new Elevator("E2", 1, 10, 5),
                new Elevator("E3", 1, 10, 10)
        );
        ElevatorControlSystem system = new InMemoryElevatorControlSystem(elevators, new NearestElevatorStrategy());

        System.out.println("== Elevator System Demo ==");
        System.out.println("Initial state: " + elevators);

        // Command: button presses become queued objects; nothing touches
        // elevator state until the control loop drains the queue in order
        System.out.println("\nButtons pressed (queued, not yet dispatched):");
        system.submit(new HallCallCommand(3, Direction.UP));
        system.submit(new HallCallCommand(8, Direction.DOWN));
        system.submit(new CarCallCommand("E1", 6));

        int step = 0;
        while (!system.allIdle() && step < 30) {
            if (step == 0) System.out.println("\nControl loop drains the command queue:");
            system.step();
            step++;
            System.out.println("Step " + step + ": " + system.getElevators());
        }
        System.out.println("\nAll elevators idle after " + step + " steps.");
    }
}
