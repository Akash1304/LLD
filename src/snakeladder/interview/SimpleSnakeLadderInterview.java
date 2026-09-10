package snakeladder.interview;

import java.util.*;

// Compact, single-file interview-friendly Snake & Ladder demo.
// Supports: N players, dice rolls, snake/ladder jumps, exact-landing win rule.
public class SimpleSnakeLadderInterview {

    static class Player {
        final String name;
        int position = 0;
        Player(String name) { this.name = name; }
    }

    static final int BOARD_SIZE = 100;

    public static void main(String[] args) {
        runDemo();
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        System.out.println("== Simple Snake & Ladder Interview Demo ==");

        // start -> end; ladders go up (end > start), snakes go down (end < start)
        Map<Integer, Integer> jumps = new HashMap<>();
        jumps.put(2, 38);
        jumps.put(7, 14);
        jumps.put(28, 84);
        jumps.put(99, 78);
        jumps.put(95, 56);
        jumps.put(62, 19);

        List<Player> players = new ArrayList<>(List.of(new Player("Alice"), new Player("Bob")));
        Random dice = new Random(42); // seeded so demo output is reproducible
        int turnLimit = 500;

        for (int turn = 0; turn < turnLimit; turn++) {
            Player current = players.get(turn % players.size());
            int roll = dice.nextInt(6) + 1;
            int target = current.position + roll;

            if (target > BOARD_SIZE) {
                System.out.printf("%s rolled %d, overshoots from %d - stays put%n", current.name, roll, current.position);
                continue;
            }

            int landed = jumps.getOrDefault(target, target);
            System.out.printf("%s rolled %d: %d -> %d%s%n", current.name, roll, current.position, target,
                    landed != target ? " -> " + landed + " (snake/ladder)" : "");
            current.position = landed;

            if (current.position == BOARD_SIZE) {
                System.out.println(current.name + " wins in " + (turn + 1) + " turns!");
                return;
            }
        }
        System.out.println("No winner after " + turnLimit + " turns (demo safety cap hit).");
    }
}
