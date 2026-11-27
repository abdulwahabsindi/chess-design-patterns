package chess.panels;

import chess.AI;
import chess.Move;
import chess.Position;
import chess.SideColor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanelAI extends GamePanel {


    public SideColor playerAI;

    public GamePanelAI() {
        chooseColor();
    }

    private void chooseColor() {
        Object[] options = {"White", "Black"};
        int response = JOptionPane.showOptionDialog(null, "Chose color you want to play", "Chose your color",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (response == 0) {
            playerAI = SideColor.BLACK;
            whiteMove ^= false;
        } else if (response == 1) {
            playerAI = SideColor.WHITE;
            whiteMove ^= true;
            opponentTurn();
        }

    }

    @Override
    void opponentTurn() {
        if (endGame) return;
        enabled = false;
        Thread t = new Thread() {
            public synchronized void run() {
                makeAIMove(playerAI);
                enabled = true;
            }

        };
        t.start();
    }

    // vs AI: undo your move and the AI's reply together, so it is your turn again.
    // Not while the AI is thinking (enabled is false then).
    @Override
    public void undo() {
        if (!enabled || history.undoCount() < 2) return;
        history.undo();
        history.undo();
        refreshAfterUndo();
    }

    @Override
    public void redo() {
        if (!enabled || history.redoCount() < 2) return;
        history.redo();
        history.redo();
        refreshAfterUndo();
    }

    void randomMove(SideColor col) {

        ArrayList<Position> pieceMoves;

        Position newposition = new Position(0, 0);
        Position oldposition = new Position(0, 0);

        int max = 0;
        int random = 0;

        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                if (piecesBoard[i][j] != null) {
                    if (piecesBoard[i][j].color == col) {
                        if (preventCheck(piecesBoard[i][j].getMoves(piecesBoard), piecesBoard, piecesBoard[i][j]).size() > 0) {
                            random = new Random().nextInt(100);
                            if (random > max) {
                                max = random;
                                oldposition.x = i;
                                oldposition.y = j;
                                pieceMoves = preventCheck(piecesBoard[i][j].getMoves(piecesBoard), piecesBoard, piecesBoard[i][j]);
                                random = new Random().nextInt(preventCheck(piecesBoard[i][j].getMoves(piecesBoard), piecesBoard, piecesBoard[i][j]).size());
                                newposition = pieceMoves.get(random);
                            }
                        }
                    }
                }
            }
        }
        moveChessman(newposition, oldposition);
    }


    public void makeAIMove(SideColor col) {

        Move move = AI.minMax(piecesBoard, col, -100000, +100000);
        if (move != null) {
            moveChessman(move.end, move.start);
        } else
            randomMove(col);
    }
}
