package elevator.interview;

import java.util.*;

// Compact, single-file interview-friendly elevator demo.
// Same design as the full version: a controller dispatching hall calls
// across 3 elevators (floors 0-9), each running direction-aware SCAN.
public class SimpleElevatorInterview {
    enum Direction { UP, DOWN, IDLE }
    enum RequestType { PICKUP_UP, PICKUP_DOWN, DESTINATION }

    record Request(int floor, RequestType type) {
        @Override public String toString() { return type + "@" + floor; }
    }

    static class Elevator {
        final String id;
        int currentFloor = 0;
        Direction direction = Direction.IDLE;
        final Set<Request> requests = new HashSet<>();

        Elevator(String id) { this.id = id; }

        boolean addRequest(Request r) {
            if (r.floor() < 0 || r.floor() > 9) return false;
            if (r.floor() == currentFloor && direction == Direction.IDLE && requests.isEmpty()) return true;
            requests.add(r);
            return true;
        }

        void step() {
            if (requests.isEmpty()) { direction = Direction.IDLE; return; }
            if (direction == Direction.IDLE) {
                Request nearest = Collections.min(requests, Comparator
                        .comparingInt((Request r) -> Math.abs(r.floor() - currentFloor))
                        .thenComparingInt(Request::floor));
                direction = nearest.floor() >= currentFloor ? Direction.UP : Direction.DOWN;
            }
            RequestType pickup = direction == Direction.UP ? RequestType.PICKUP_UP : RequestType.PICKUP_DOWN;
            boolean stopped = requests.remove(new Request(currentFloor, RequestType.DESTINATION));
            stopped |= requests.remove(new Request(currentFloor, pickup));
            if (stopped) return;

            boolean ahead = requests.stream().anyMatch(r ->
                    direction == Direction.UP ? r.floor() > currentFloor : r.floor() < currentFloor);
            if (!ahead) { direction = direction == Direction.UP ? Direction.DOWN : Direction.UP; return; }
            currentFloor += direction == Direction.UP ? 1 : -1;
        }

        @Override public String toString() { return id + "[" + currentFloor + " " + direction + " " + requests + "]"; }
    }

    static class ElevatorController {
        final List<Elevator> elevators = List.of(new Elevator("E1"), new Elevator("E2"), new Elevator("E3"));

        boolean requestElevator(int floor, RequestType type) {
            if (floor < 0 || floor > 9 || type == RequestType.DESTINATION) return false;
            Direction wanted = type == RequestType.PICKUP_UP ? Direction.UP : Direction.DOWN;
            Elevator best = nearest(floor, e -> e.direction == wanted
                    && (wanted == Direction.UP ? e.currentFloor <= floor : e.currentFloor >= floor));
            if (best == null) best = nearest(floor, e -> e.requests.isEmpty());
            if (best == null) best = nearest(floor, e -> true);
            return best.addRequest(new Request(floor, type));
        }

        Elevator nearest(int floor, java.util.function.Predicate<Elevator> filter) {
            return elevators.stream().filter(filter)
                    .min(Comparator.comparingInt(e -> Math.abs(e.currentFloor - floor))).orElse(null);
        }

        void step() { elevators.forEach(Elevator::step); }

        boolean allIdle() { return elevators.stream().allMatch(e -> e.requests.isEmpty()); }
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        System.out.println("== Simple Elevator Interview Demo ==");
        ElevatorController controller = new ElevatorController();
        controller.requestElevator(5, RequestType.PICKUP_UP);
        controller.requestElevator(3, RequestType.PICKUP_DOWN);
        controller.elevators.get(0).addRequest(new Request(8, RequestType.DESTINATION));
        System.out.println("Initial: " + controller.elevators);

        int step = 0;
        while (!controller.allIdle() && step < 30) {
            controller.step();
            step++;
            System.out.println("Step " + step + ": " + controller.elevators);
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
