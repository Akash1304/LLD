package snakeladder.service;

import snakeladder.GameStatus;
import snakeladder.models.Board;
import snakeladder.models.Dice;
import snakeladder.models.Player;

import java.util.List;

public class InMemoryGameService implements GameService {
    private final Board board;
    private final Dice dice;
    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private GameStatus status;
    private Player winner;

    public InMemoryGameService(Board board, Dice dice, List<Player> players) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players");
        }
        this.board = board;
        this.dice = dice;
        this.players = players;
        this.status = GameStatus.IN_PROGRESS;
    }

    @Override
    public synchronized GameStatus getStatus() {
        return status;
    }

    @Override
    public synchronized Player getWinner() {
        return winner;
    }

    // synchronized on the whole method: a board game session has exactly
    // one shared mutable turn state (whose go it is, each player's
    // position), and turns are inherently sequential anyway -- there's no
    // finer-grained resource to split the lock across the way ParkingSpot
    // or Show do in other LLDs. One lock per game instance (not a global
    // lock across all games) is already the correct granularity here.
    @Override
    public synchronized Player playTurn() {
        if (status == GameStatus.FINISHED) {
            return null;
        }

        Player current = players.get(currentPlayerIndex);
        int roll = dice.roll();
        int target = current.getPosition() + roll;

        if (target > board.getSize()) {
            System.out.printf("%s rolled %d, overshoots from %d - stays put%n",
                    current.getName(), roll, current.getPosition());
        } else {
            int finalPosition = board.getFinalPosition(target);
            if (finalPosition != target) {
                System.out.printf("%s rolled %d: %d -> %d, hit a snake/ladder -> %d%n",
                        current.getName(), roll, current.getPosition(), target, finalPosition);
            } else {
                System.out.printf("%s rolled %d: %d -> %d%n",
                        current.getName(), roll, current.getPosition(), target);
            }
            current.setPosition(finalPosition);

            if (finalPosition == board.getSize()) {
                status = GameStatus.FINISHED;
                winner = current;
                return current;
            }
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return current;
    }
}
