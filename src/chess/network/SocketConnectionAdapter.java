package chess.network;

import chess.Position;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Adapter pattern - ADAPTER (object adapter).
 * Implements the game's OpponentConnection interface (the target) on top of
 * Java's Socket, ServerSocket and object streams (the adaptees).
 *
 * It translates in both directions:
 *  - sendMove(from, to)  ->  writeObject(from), writeObject(to) on the socket
 *  - two readObject() calls on a background thread  ->  listener.onMoveReceived(from, to)
 */
public class SocketConnectionAdapter implements OpponentConnection {

    private final Socket socket;
    private final ServerSocket serverSocket; // only set on the hosting side
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean closed = false;

    private SocketConnectionAdapter(Socket socket, ServerSocket serverSocket) throws IOException {
        this.socket = socket;
        this.serverSocket = serverSocket;
        // Both sides create the output stream first, so neither blocks
        // waiting for the other's stream header (same order as the original code).
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Same rule as the original GamePanelLAN: first try to join a game at the
     * address; if nobody is there, host a game on that port and wait
     * (blocking) until the other player joins.
     */
    public static SocketConnectionAdapter connectOrHost(InetAddress address, int port) throws IOException {
        try {
            return new SocketConnectionAdapter(new Socket(address, port), null);
        } catch (IOException noGameToJoin) {
            ServerSocket server = new ServerSocket(port);
            Socket client = server.accept();
            return new SocketConnectionAdapter(client, server);
        }
    }

    @Override
    public void sendMove(Position from, Position to) {
        try {
            out.writeObject(from);
            out.flush();
            out.writeObject(to);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startListening(MoveListener listener) {
        Thread receiver = new Thread(() -> {
            while (!closed) {
                try {
                    Position from = (Position) in.readObject();
                    Position to = (Position) in.readObject();
                    listener.onMoveReceived(from, to);
                } catch (ClassNotFoundException | IOException e) {
                    listener.onConnectionLost();
                    return;
                }
            }
        }, "lan-receiver");
        receiver.start();
    }

    @Override
    public boolean isHost() {
        return serverSocket != null;
    }

    @Override
    public void close() {
        closed = true;
        try {
            out.close();
            in.close();
            socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
