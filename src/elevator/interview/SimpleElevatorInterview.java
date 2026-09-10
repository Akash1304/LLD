package elevator.interview;

import java.util.*;

// Compact, single-file interview-friendly elevator demo.
// Supports: a single elevator running the SCAN algorithm over hall calls
// and car calls, printing its position after each step.
public class SimpleElevatorInterview {
    enum Direction { UP, DOWN, IDLE }

    static class Elevator {
        int currentFloor;
        final int minFloor;
        final int maxFloor;
        Direction direction = Direction.IDLE;
        final TreeSet<Integer> upStops = new TreeSet<>();
        final TreeSet<Integer> downStops = new TreeSet<>();

        Elevator(int minFloor, int maxFloor, int startFloor) {
            this.minFloor = minFloor; this.maxFloor = maxFloor; this.currentFloor = startFloor;
        }

        void requestStop(int floor) {
            if (floor == currentFloor) return;
            if (floor > currentFloor) upStops.add(floor); else downStops.add(floor);
            if (direction == Direction.IDLE) direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }

        void step() {
            if (direction == Direction.IDLE) return;
            if (direction == Direction.UP) {
                if (currentFloor < maxFloor) currentFloor++;
                if (upStops.remove(currentFloor) && upStops.isEmpty()) {
                    direction = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN;
                }
            } else {
                if (currentFloor > minFloor) currentFloor--;
                if (downStops.remove(currentFloor) && downStops.isEmpty()) {
                    direction = upStops.isEmpty() ? Direction.IDLE : Direction.UP;
                }
            }
        }

        @Override public String toString() {
            return "floor=" + currentFloor + ", dir=" + direction + ", up=" + upStops + ", down=" + downStops;
        }
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        System.out.println("== Simple Elevator Interview Demo ==");
        Elevator elevator = new Elevator(1, 10, 1);

        elevator.requestStop(5);
        elevator.requestStop(3);
        elevator.requestStop(8);
        System.out.println("Requests queued: floor 5 (car call), floor 3 (hall call up), floor 8 (hall call down)");
        System.out.println("Initial: " + elevator);

        int step = 0;
        while (elevator.direction != Direction.IDLE && step < 20) {
            elevator.step();
            step++;
            System.out.println("Step " + step + ": " + elevator);
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
