package chess.panels;

import chess.GameFrame;
import chess.Position;
import chess.network.MoveListener;
import chess.network.OpponentConnection;
import chess.network.SocketConnectionAdapter;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Adapter pattern - CLIENT.
 * The LAN game only talks to an OpponentConnection. All socket and stream
 * code now lives in SocketConnectionAdapter.
 */
public class GamePanelLAN extends GamePanel implements MoveListener {

    private OpponentConnection connection;


    public GamePanelLAN() {
        getSocketIP();
    }

    public void getSocketIP() {
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
                int input = JOptionPane.showConfirmDialog(null,
                        "Input data is not valid !", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                getSocketIP();
            }
        } else {
            GameFrame gf = ((GameFrame) this.getParent());
            gf.setVisible(false);
            gf.dispose();
        }
    }


    public void connect(InetAddress ipAdd, int port) throws IOException {
        connection = SocketConnectionAdapter.connectOrHost(ipAdd, port);

        if (connection.isHost()) {
            connection.startListening(this);
            JOptionPane.showConfirmDialog(null,
                    "You play as Whites", "Connected !", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showConfirmDialog(null,
                    "You play as Blacks", "Connected !", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
            whiteMove = false;
            opponentTurn();
            connection.startListening(this);
        }
    }

    // ---- MoveListener: called by the connection's receiver thread ----

    @Override
    public void onMoveReceived(Position from, Position to) {
        super.moveChessman(to, from); // super: apply the move without sending it back
        repaint();
        enabled = true;
    }

    @Override
    public void onConnectionLost() {
        if (endGame) return;
        JOptionPane.showConfirmDialog(null,
                "Connection is lost. The game is over", "Disconection",
                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        closeFrame();
    }

    // ---- the local player's move: apply it, then send it ----

    @Override
    public void moveChessman(Position newPosition, Position currentPosition) {
        super.moveChessman(newPosition, currentPosition);
        connection.sendMove(currentPosition, newPosition);
        opponentTurn();
    }

    @Override
    public void closeFrame() {
        if (connection != null) connection.close();
        super.closeFrame();
    }


    @Override
    void opponentTurn() {
        enabled = false;
    }

}
