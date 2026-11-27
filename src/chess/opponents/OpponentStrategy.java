package chess.opponents;

import chess.Position;
import chess.moves.MoveHistory;
import chess.panels.GamePanel;

/**
 * Strategy pattern - STRATEGY.
 * How the other side of the board plays: a second person on the same
 * computer, the AI, or a player on another computer.
 * GamePanel (the context) holds one strategy and calls it; it does not
 * know which kind of opponent it is playing against.
 */
public interface OpponentStrategy {

    /**
     * Called once, when the game screen has set up the board and before
     * the first move. Ask the player for a side, connect, and so on.
     *
     * @throws java.util.concurrent.CancellationException if the player
     *         cancels, so that no game window is opened
     */
    void start(GamePanel game);

    /**
     * The local player has just moved a piece from one square to another.
     * It is now the opponent's turn.
     */
    void opponentTurn(Position from, Position to);

    /** Edit > Undo: take back moves from the history, in the way this kind of game allows. */
    void undo(MoveHistory history);

    /** Edit > Redo: the opposite of undo. */
    void redo(MoveHistory history);

    /** The game screen is closing. Release anything the opponent holds. */
    void stop();
}
