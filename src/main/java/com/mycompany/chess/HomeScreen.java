package com.mycompany.chess;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Main menu window (“home screen”) for each client.
 */
public class HomeScreen extends JFrame {

    private int clientId = -1;
    private GameClient client;           
    private JLabel idLabel;                // label we update once id known
    private JDialog searchingDlg;           // “Looking for opponent…”
    private boolean waiting = false;     // true ⇢ already in queue

    /* ================================================================
       1.  No-arg constructor – used by ClientMain
       ================================================================ */
    HomeScreen(int id, GameClient client) {
        this.clientId = id;
        this.client = client;
        buildUi();
        setVisible(true);
    }

    /* ================================================================
       Build the entire UI  (called by both ctors)
       ================================================================ */
    private void buildUi() {
        setTitle("Online Chess — Client #" + clientId);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        /* ---------- dark sidebar ---------------------------------- */
        JPanel side = new JPanel();
        side.setBackground(new Color(34, 34, 34));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(180, 1));
        side.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel logo = new JLabel("♔");
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(48f));

        idLabel = new JLabel("Client #" + clientId, SwingConstants.CENTER);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        idLabel.setForeground(new Color(200, 200, 200));
        idLabel.setBorder(new EmptyBorder(10, 0, 30, 0));

        JButton btnPlay = menuButton("Find match", e -> findMatch());
        JButton btnQuit = menuButton("Quit", e -> System.exit(0));

        side.add(logo);
        side.add(idLabel);
        side.add(Box.createVerticalGlue());
        side.add(btnPlay);
        side.add(Box.createVerticalStrut(10));
        side.add(btnQuit);
        side.add(Box.createVerticalGlue());

        JPanel glass = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                        new Color(255, 255, 255, 150),
                        0, getHeight(),
                        new Color(255, 255, 255, 80));
                g2.setPaint(gp);
                g2.fillRoundRect(30, 30, getWidth() - 60, getHeight() - 60, 25, 25);
            }
        };
        glass.setOpaque(false);
        JLabel art = new JLabel("Online  •  Chess", SwingConstants.CENTER);
        art.setFont(art.getFont().deriveFont(Font.BOLD, 38f));
        art.setForeground(new Color(60, 60, 60));
        glass.add(art, BorderLayout.CENTER);

        add(side, BorderLayout.WEST);
        add(glass, BorderLayout.CENTER);
    }

    private static JButton menuButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.addActionListener(al);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }

    /* ================================================================
       Match-making flow
       ================================================================ */
    private void findMatch() {
        if (waiting) {
            return; 
        }
        waiting = true;
        client.findNewGame();

        searchingDlg = new JDialog(this, "Searching…", true);
        searchingDlg.setSize(300, 140);
        searchingDlg.setLocationRelativeTo(this);
        searchingDlg.add(new JLabel("Looking for opponent…",
                SwingConstants.CENTER), BorderLayout.CENTER);
        searchingDlg.setVisible(true);
    }

    /* called from GameClient on “GAME_START …” */
    public void onGameStart() {
        SwingUtilities.invokeLater(() -> {
            waiting = false;
            if (searchingDlg != null) {
                searchingDlg.dispose();
            }
            setVisible(false);              // hide menu while playing
        });
    }

    /* called from GameClient on “GAME_OVER …” */
    public void onGameOver(String result) {
        SwingUtilities.invokeLater(() -> {

            JDialog dlg = new JDialog(this, false);
            dlg.setUndecorated(true);
            dlg.setOpacity(0.90f);
            dlg.setSize(330, 160);
            dlg.setLocationRelativeTo(null);
            dlg.setLayout(new BorderLayout());

            final Point[] anchor = {null};
            dlg.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    anchor[0] = e.getPoint();
                }
            });
            dlg.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (anchor[0] == null) {
                        return;
                    }

                    int newX = dlg.getX() + e.getX() - anchor[0].x;
                    int newY = dlg.getY() + e.getY() - anchor[0].y;

                    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getMaximumWindowBounds();
                    int minX = screen.x + 40 - dlg.getWidth();
                    int minY = screen.y + 40 - dlg.getHeight();
                    int maxX = screen.x + screen.width - 40;
                    int maxY = screen.y + screen.height - 40;

                    newX = Math.max(minX, Math.min(newX, maxX));
                    newY = Math.max(minY, Math.min(newY, maxY));

                    dlg.setLocation(newX, newY);
                }
            });

            JPanel msg = new JPanel(new GridLayout(3, 1));
            msg.add(new JLabel("Client #" + clientId, SwingConstants.CENTER));
            msg.add(new JLabel(result, SwingConstants.CENTER));
            msg.add(new JLabel("What would you like to do?",
                    SwingConstants.CENTER));
            dlg.add(msg, BorderLayout.CENTER);

            JPanel btns = new JPanel();
            JButton again = new JButton("Find new match");
            JButton back = new JButton("Back to menu");
            btns.add(again);
            btns.add(back);
            dlg.add(btns, BorderLayout.SOUTH);

            again.addActionListener(e -> {
                dlg.dispose();
                client.closeBoardWindow();      // board disappears now
                findMatch();
            });

            back.addActionListener(e -> {
                dlg.dispose();
                client.closeBoardWindow();
                waiting = false;
                setVisible(true);
            });

            dlg.setVisible(true);
        });
    }
}
