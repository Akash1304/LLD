package snakeladder.driver;

import snakeladder.GameStatus;
import snakeladder.models.*;
import snakeladder.service.GameService;
import snakeladder.service.InMemoryGameService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnakeLadderDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        List<BoardEntity> entities = new ArrayList<>(Arrays.asList(
                new Ladder(2, 38),
                new Ladder(7, 14),
                new Ladder(28, 84),
                new Snake(99, 78),
                new Snake(95, 56),
                new Snake(62, 19)
        ));
        Board board = new Board(100, entities);
        Dice dice = new Dice(1, 6);
        List<Player> players = new ArrayList<>(Arrays.asList(new Player("Alice"), new Player("Bob")));

        GameService game = new InMemoryGameService(board, dice, players);

        System.out.println("== Snake & Ladder ==");
        int turnLimit = 1000; // safety valve so a pathological board can't loop forever in the demo
        int turns = 0;
        while (game.getStatus() != GameStatus.FINISHED && turns < turnLimit) {
            game.playTurn();
            turns++;
        }

        if (game.getWinner() != null) {
            System.out.println(game.getWinner().getName() + " wins in " + turns + " turns!");
        } else {
            System.out.println("No winner after " + turnLimit + " turns (demo safety cap hit).");
        }
    }
}
