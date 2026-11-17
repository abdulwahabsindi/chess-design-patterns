package chess.network;

import chess.Position;

/**
 * Callback the game gives to an OpponentConnection, so the connection can
 * report incoming moves without knowing anything about the game or the UI.
 */
public interface MoveListener {

    /** The opponent moved a piece from one square to another. */
    void onMoveReceived(Position from, Position to);

    /** The connection broke (the opponent left or the network failed). */
    void onConnectionLost();
}
