package snakeladder.service;

import snakeladder.GameStatus;
import snakeladder.models.Player;

public interface GameService {
    GameStatus getStatus();

    // plays one turn for the current player and advances the turn order;
    // returns the player who just moved, or null if the game already finished
    Player playTurn();

    Player getWinner();
}
