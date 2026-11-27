package chess.panels;

public class GamePanelHot extends GamePanel {

    @Override
    void opponentTurn() {
        whiteMove ^= true;

    }

    // Hot-seat: one Undo takes back one move, so the turn goes back to the other player.
    @Override
    public void undo() {
        if (!history.canUndo()) return;
        history.undo();
        whiteMove ^= true;
        refreshAfterUndo();
    }

    @Override
    public void redo() {
        if (!history.canRedo()) return;
        history.redo();
        whiteMove ^= true;
        refreshAfterUndo();
    }
}
