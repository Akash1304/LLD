package elevator.driver;

import elevator.controller.ElevatorController;
import elevator.model.Elevator;
import elevator.model.Request;
import elevator.model.RequestType;
import elevator.selectElevatorStrategy.SelectNearest;

public class ElevatorDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        // Step:1 create controller - 3 elevators, floors 0-9, all at floor 0
        ElevatorController controller = new ElevatorController(new SelectNearest());
        System.out.println("== Elevator System Demo ==");
        System.out.println("Initial: " + controller.getElevators());

        // Step:2 invalid requests are rejected
        System.out.println("\nInvalid hall call floor 12: " + controller.requestElevator(12, RequestType.PICKUP_UP));
        System.out.println("DESTINATION via hall call: " + controller.requestElevator(4, RequestType.DESTINATION));

        // Step:3 hall calls - all elevators idle at 0, so nearest idle wins
        System.out.println("\nHall call: floor 5 UP   -> " + controller.requestElevator(5, RequestType.PICKUP_UP));
        System.out.println("Hall call: floor 3 DOWN -> " + controller.requestElevator(3, RequestType.PICKUP_DOWN));
        System.out.println("After dispatch: " + controller.getElevators());

        runUntil(controller, 3);

        // Step:4 E1 is now heading UP below floor 7 - tier 1 picks it
        // ("already moving toward you in your direction")
        System.out.println("\nHall call: floor 7 UP   -> " + controller.requestElevator(7, RequestType.PICKUP_UP));
        System.out.println("After dispatch: " + controller.getElevators());

        runUntil(controller, 3);

        // Step:5 passenger who boarded E1 at 5 presses 9 and 2 inside the car.
        // E1 finishes its UP sweep (7, 9) before reversing for 2 - that's SCAN.
        Elevator e1 = controller.getElevator(0);
        e1.addRequest(new Request(9, RequestType.DESTINATION));
        e1.addRequest(new Request(2, RequestType.DESTINATION));
        System.out.println("\nE1 car buttons 9 and 2: " + e1);

        int steps = runUntil(controller, 50);
        System.out.println("\nAll elevators idle after " + steps + " more steps.");
    }

    private static int runUntil(ElevatorController controller, int maxSteps) {
        int step = 0;
        while (!controller.allIdle() && step < maxSteps) {
            controller.step();
            step++;
            System.out.println("  tick " + step + ": " + controller.getElevators());
        }
        return step;
    }
}
