package chess.opponents;

import chess.Position;
import chess.moves.MoveHistory;
import chess.network.MoveListener;
import chess.network.OpponentConnection;
import chess.network.SocketConnectionAdapter;
import chess.panels.GamePanel;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;

/**
 * Strategy pattern - CONCRETE STRATEGY: a player on another computer.
 * On its turn it sends the local move over the network and locks the board
 * until the other player's move arrives.
 * It talks to the network only through OpponentConnection, the Stage 2
 * Adapter, which it uses unchanged. (Was GamePanelLAN.)
 */
public class NetworkOpponent implements OpponentStrategy, MoveListener {

    private GamePanel game;
    private OpponentConnection connection;

    @Override
    public void start(GamePanel game) {
        this.game = game;
        getSocketIP();
    }

    private void getSocketIP() {
        JPanel p = new JPanel();
        JTextField IPInput = new JTextField(10);
        JTextField PortInput = new JTextField(10);
        PortInput.setText("1234");
        IPInput.setText("127.0.0.1");

        p.add(new JLabel("IP :"));
        p.add(IPInput);
        p.add(new JLabel("Port : "));
        p.add(PortInput);
        int resp = JOptionPane.showConfirmDialog(null, p, "Connection", JOptionPane.DEFAULT_OPTION);
        if (resp == 0) {
            try {
                if (PortInput.getText().isEmpty()) {
                    throw new UnknownHostException("Inputs cannot be empty");
                }
                InetAddress ipAdd = InetAddress.getByName(IPInput.getText());
                int port = Integer.parseInt(PortInput.getText());
                connect(ipAdd, port);

            } catch (Exception e) {
                JOptionPane.showConfirmDialog(null,
                        "Input data is not valid !", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                getSocketIP();
            }
        } else {
            // The player closed the dialog: no game window is opened and the menu stays.
            throw new CancellationException("Connection cancelled");
        }
    }

    private void connect(InetAddress ipAdd, int port) throws IOException {
        connection = SocketConnectionAdapter.connectOrHost(ipAdd, port);

        if (connection.isHost()) {
            connection.startListening(this);
            JOptionPane.showConfirmDialog(null,
                    "You play as Whites", "Connected !", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showConfirmDialog(null,
                    "You play as Blacks", "Connected !", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
            game.whiteMove = false;
            game.enabled = false;
            connection.startListening(this);
        }
    }

    @Override
    public void opponentTurn(Position from, Position to) {
        connection.sendMove(from, to);
        game.enabled = false;
    }

    // ---- MoveListener: called by the connection's receiver thread ----

    @Override
    public void onMoveReceived(Position from, Position to) {
        // Applied straight to the board; it does not pass through opponentTurn,
        // so it is never sent back to the player who made it.
        game.moveChessman(to, from);
        game.repaint();
        game.enabled = true;
    }

    @Override
    public void onConnectionLost() {
        if (game.isGameOver()) return;
        JOptionPane.showConfirmDialog(null,
                "Connection is lost. The game is over", "Disconection",
                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        game.closeFrame();
    }

    // LAN: undo would make the two boards differ, so it is turned off.
    @Override
    public void undo(MoveHistory history) {
        showUndoUnavailable();
    }

    @Override
    public void redo(MoveHistory history) {
        showUndoUnavailable();
    }

    private void showUndoUnavailable() {
        JOptionPane.showMessageDialog(game, "Undo and Redo are not available in LAN games.",
                "Not available", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void stop() {
        if (connection != null) connection.close();
    }
}
