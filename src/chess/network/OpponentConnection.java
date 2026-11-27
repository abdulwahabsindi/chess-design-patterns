package chess.network;

import chess.Position;

/**
 * Adapter pattern - TARGET.
 * The interface the game wants: "a connection to an opponent" that can send
 * and receive moves. The game never sees sockets or object streams.
 */
public interface OpponentConnection {

    /** Sends the local player's move to the opponent. */
    void sendMove(Position from, Position to);

    /** Starts listening for the opponent's moves; each one is passed to the listener. */
    void startListening(MoveListener listener);

    /** True if this side opened the game and waited for the other player (plays White). */
    boolean isHost();

    /** Closes the connection. */
    void close();
}
