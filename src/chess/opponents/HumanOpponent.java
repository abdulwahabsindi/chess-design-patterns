package chess.opponents;

import chess.Position;
import chess.panels.GamePanel;

/**
 * Strategy pattern - CONCRETE STRATEGY: hot-seat.
 * The opponent is a second person at the same computer, so "the opponent's
 * turn" only means handing the board to the other colour.
 * (Was GamePanelHot.)
 */
public class HumanOpponent implements OpponentStrategy {

    private GamePanel game;

    @Override
    public void start(GamePanel game) {
        this.game = game;
    }

    @Override
    public void opponentTurn(Position from, Position to) {
        game.whiteMove ^= true;
    }

    @Override
    public void stop() {
        // nothing to release
    }
}
