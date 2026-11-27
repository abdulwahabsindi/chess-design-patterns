package chess.opponents;

import chess.Position;
import chess.moves.MoveHistory;
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

    // Hot-seat: one Undo takes back one move, so the turn goes back to the other player.
    @Override
    public void undo(MoveHistory history) {
        if (!history.canUndo()) return;
        history.undo();
        game.whiteMove ^= true;
        game.refreshAfterUndo();
    }

    @Override
    public void redo(MoveHistory history) {
        if (!history.canRedo()) return;
        history.redo();
        game.whiteMove ^= true;
        game.refreshAfterUndo();
    }

    @Override
    public void stop() {
        // nothing to release
    }
}
