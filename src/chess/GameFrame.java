package chess;


import chess.panels.GamePanel;

import javax.swing.*;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;


public class GameFrame extends JFrame {

    public GameFrame(GamePanel gamePanel) {
        super("Chess");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.add(gamePanel);


        this.setJMenuBar(createMenuBar(gamePanel));
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

    }

    // Edit menu: the menu items only call undo()/redo(); each panel decides what that means.
    private JMenuBar createMenuBar(GamePanel gamePanel) {
        int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut));
        undoItem.addActionListener(e -> gamePanel.undo());

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcut | InputEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> gamePanel.redo());

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoItem);
        editMenu.add(redoItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(editMenu);
        return menuBar;
    }

}
