package chess.opponents;

import chess.AI;
import chess.Move;
import chess.Position;
import chess.SideColor;
import chess.moves.MoveHistory;
import chess.panels.GamePanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Strategy pattern - CONCRETE STRATEGY: the computer.
 * On its turn it locks the board, picks a move with AI.minMax on a
 * background thread, plays it, and unlocks the board.
 * (Was GamePanelAI.)
 */
public class AIOpponent implements OpponentStrategy {

    private GamePanel game;
    private SideColor playerAI;

    @Override
    public void start(GamePanel game) {
        this.game = game;
        chooseColor();
    }

    private void chooseColor() {
        Object[] options = {"White", "Black"};
        int response = JOptionPane.showOptionDialog(null, "Chose color you want to play", "Chose your color",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (response == 0) {
            playerAI = SideColor.BLACK;
            game.whiteMove ^= false;
        } else if (response == 1) {
            playerAI = SideColor.WHITE;
            game.whiteMove ^= true;
            play();
        }
    }

    @Override
    public void opponentTurn(Position from, Position to) {
        play();
    }

    private void play() {
        if (game.isGameOver()) return;
        game.enabled = false;
        Thread t = new Thread() {
            public synchronized void run() {
                makeAIMove(playerAI);
                game.enabled = true;
            }
        };
        t.start();
    }

    private void makeAIMove(SideColor col) {
        Move move = AI.minMax(game.piecesBoard, col, -100000, +100000);
        if (move != null) {
            game.moveChessman(move.end, move.start);
        } else
            randomMove(col);
    }

    private void randomMove(SideColor col) {
        // Same rule as GamePanelAI.randomMove: pick a random piece that has a legal move,
        // then a random legal move for it. Only used if minMax finds no move.
        ArrayList<Position> pieceMoves;
        Position newposition = new Position(0, 0);
        Position oldposition = new Position(0, 0);
        int max = 0;
        int random;

        for (int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                if (game.piecesBoard[i][j] != null && game.piecesBoard[i][j].color == col) {
                    pieceMoves = GamePanel.preventCheck(game.piecesBoard[i][j].getMoves(game.piecesBoard),
                            game.piecesBoard, game.piecesBoard[i][j]);
                    if (pieceMoves.size() > 0) {
                        random = new Random().nextInt(100);
                        if (random > max) {
                            max = random;
                            oldposition.x = i;
                            oldposition.y = j;
                            newposition = pieceMoves.get(new Random().nextInt(pieceMoves.size()));
                        }
                    }
                }
            }
        }
        game.moveChessman(newposition, oldposition);
    }

    // vs AI: undo your move and the AI's reply together, so it is your turn again.
    // Not while the AI is thinking (enabled is false then).
    @Override
    public void undo(MoveHistory history) {
        if (!game.enabled || history.undoCount() < 2) return;
        history.undo();
        history.undo();
        game.refreshAfterUndo();
    }

    @Override
    public void redo(MoveHistory history) {
        if (!game.enabled || history.redoCount() < 2) return;
        history.redo();
        history.redo();
        game.refreshAfterUndo();
    }

    @Override
    public void stop() {
        // nothing to release
    }
}
