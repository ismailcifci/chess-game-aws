
package com.mycompany.chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameScreen extends JFrame {

    private final ChessGame.Color playerColor;
    private String lastMoveSent = null;
    private boolean initialStateApplied = false;
    private boolean initialSyncDone = false;

    // awt.Color for GUI colors
    private static final java.awt.Color CAPTURE_RED = new java.awt.Color(255, 110, 110);
    private static final java.awt.Color GUI_WHITE = new java.awt.Color(240, 217, 181);
    private static final java.awt.Color GUI_BLACK = new java.awt.Color(181, 136, 99);
    private static final java.awt.Color HIGHLIGHT_YELLOW = new java.awt.Color(255, 255, 150);
    private static final java.awt.Color VALID_MOVE_BLUE = new java.awt.Color(150, 200, 255);
    private static final Font PIECE_FONT = new Font(Font.SERIF, Font.PLAIN, 36);

    private final JPanel boardPanel;

    private final GameClient client;
    private ChessGame.Position selectedPosition;
    private final List<ChessGame.Position> validMoves = new ArrayList<>();
    private ChessGame gameState;

    public GameScreen(String color, GameClient client, int id) {
        this.playerColor = color.equalsIgnoreCase("WHITE")
                ? ChessGame.Color.WHITE
                : ChessGame.Color.BLACK;
        this.client = client;
        this.gameState = new ChessGame();
        this.selectedPosition = null;

        setTitle("Chess (" + color + ") — Client #" + client.getMyId());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);

        boardPanel = new JPanel(new GridLayout(8, 8)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension parentSize = getParent().getSize();
                int size = Math.min(parentSize.width, parentSize.height);
                return new Dimension(size, size);
            }
        };
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializeBoard();
        add(boardPanel, BorderLayout.CENTER);

    }

    private void initializeBoard() {
        boardPanel.removeAll();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = createSquare(row, col);
                boardPanel.add(square);
            }
        }
        refreshBoard();
    }

    private JButton createSquare(int row, int col) {
        JButton square = new JButton();
        square.setOpaque(true);
        square.setBorder(BorderFactory.createLineBorder(new java.awt.Color(169, 169, 169)));
        square.setContentAreaFilled(true);
        square.setFocusPainted(false);
        square.setFont(PIECE_FONT);
        square.setHorizontalAlignment(SwingConstants.CENTER);
        square.setVerticalAlignment(SwingConstants.CENTER);
        square.addMouseListener(new SquareClickListener(row, col));
        return square;
    }

    public void closeSilently() {
        dispose();
    }

    private void refreshBoard() {
        for (int uiRow = 0; uiRow < 8; uiRow++) {
            for (int uiCol = 0; uiCol < 8; uiCol++) {
                JButton sq = (JButton) boardPanel.getComponent(uiRow * 8 + uiCol);
                updateSquareAppearance(sq, uiRow, uiCol);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private void updateSquareAppearance(JButton square, int row, int col) {
        // Get chess piece color
        ChessGame.Piece piece
                = gameState.getPiece(new ChessGame.Position(uiRow(row), uiCol(col)));

        // Set GUI background color
        java.awt.Color bgColor = ((row + col) % 2 == 0) ? GUI_WHITE : GUI_BLACK;
        square.setBackground(bgColor);

        // Set piece color (foreground)
        square.setText(getPieceSymbol(piece));
        if (piece != null) {
            square.setForeground(piece.color == ChessGame.Color.WHITE
                    ? java.awt.Color.WHITE
                    : java.awt.Color.BLACK);
        }

        // Highlighting
        if (selectedPosition != null) {

            /* convert the *model* coordinates we store into UI coords */
            int selUiRow = uiRow(selectedPosition.row);
            int selUiCol = uiCol(selectedPosition.col);

            if (selUiRow == row && selUiCol == col) {
                /* origin square = yellow */
                square.setBackground(HIGHLIGHT_YELLOW);
            } else {
                /* is this square one of the legal targets? */
                boolean legal = validMoves.stream().anyMatch(p
                        -> uiRow(p.row) == row && uiCol(p.col) == col);

                if (legal) {
                    /* does an enemy piece sit here? -> capture */
                    ChessGame.Piece tgt = gameState.getPiece(
                            new ChessGame.Position(modelRow(row), modelCol(col)));

                    if (tgt != null && tgt.color != playerColor) {
                        square.setBackground(CAPTURE_RED);      // capture!
                    } else {
                        square.setBackground(VALID_MOVE_BLUE);  // quiet move
                    }
                }
            }
        }
    }

    private String getPieceSymbol(ChessGame.Piece piece) {
        if (piece == null) {
            return "";
        }
        switch (piece.type) {
            case KING:
                return piece.color == ChessGame.Color.WHITE ? "♔" : "♚";
            case QUEEN:
                return piece.color == ChessGame.Color.WHITE ? "♕" : "♛";
            case ROOK:
                return piece.color == ChessGame.Color.WHITE ? "♖" : "♜";
            case BISHOP:
                return piece.color == ChessGame.Color.WHITE ? "♗" : "♝";
            case KNIGHT:
                return piece.color == ChessGame.Color.WHITE ? "♘" : "♞";
            case PAWN:
                return piece.color == ChessGame.Color.WHITE ? "♙" : "♟";
            default:
                return "";
        }
    }

    private class SquareClickListener extends MouseAdapter {

        private final int row;
        private final int col;

        public SquareClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!isPlayersTurn()) {
                return;
            }

            ChessGame.Position clickedPosition = new ChessGame.Position(uiRow(row), uiCol(col));
            ChessGame.Piece clickedPiece = gameState.getPiece(clickedPosition);

            if (clickedPiece != null && clickedPiece.color == playerColor) {
                handlePieceSelection(clickedPosition, clickedPiece);   // start new selection
            } else if (selectedPosition != null) {
                handleMoveAttempt(clickedPosition);                    // try to move
            }
        }

        private void handlePieceSelection(ChessGame.Position position, ChessGame.Piece piece) {
            if (piece != null && piece.color == playerColor) {
                selectedPosition = position;
                validMoves.clear();
                validMoves.addAll(calculateValidMoves(position));
                refreshBoard();
            }
        }

        private void handleMoveAttempt(ChessGame.Position targetPosition) {
            if (validMoves.contains(targetPosition)) {
                String move = createMoveString(targetPosition);

                if (isPawnPromotion(move)) {
                    handlePromotion(move);         // that method will call sendMoveToServer()
                } else {
                    sendMoveToServer(move);
                }

                gameState.applyMove(move);
                refreshBoard();

                lastMoveSent = move;
                resetSelection();
            }
        }

        private String createMoveString(ChessGame.Position target) {
            return positionToChessNotation(selectedPosition) + "-"
                    + positionToChessNotation(target);
        }

        private void resetSelection() {
            selectedPosition = null;
            validMoves.clear();
            refreshBoard();
        }
    }

    private List<ChessGame.Position> calculateValidMoves(ChessGame.Position pos) {
        return gameState.getLegalMoves(pos);
    }

    private boolean isPawnPromotion(String move) {
        ChessGame.Position target = new ChessGame.Position(move.split("-")[1]);
        ChessGame.Piece piece = gameState.getPiece(new ChessGame.Position(move.split("-")[0]));
        return piece != null
                && piece.type == ChessGame.PieceType.PAWN
                && (target.row == 0 || target.row == 7);
    }

    private void handlePromotion(String move) {
        sendMoveToServer(move);
    }

    private void sendMoveToServer(String move) {
        client.sendMove(move);
    }

    private String positionToChessNotation(ChessGame.Position pos) {
        char file = (char) ('a' + pos.col);
        int rank = 8 - pos.row;
        return "" + file + rank;
    }

    private boolean isPlayersTurn() {
        return gameState.getCurrentTurn() == playerColor;
    }

    public void handleServerMessage(String message) {
        if (message.startsWith("MOVE")) {
            String move = message.substring(5).trim();
            applyMove(move);
            return;

        } else if (message.startsWith("BOARD_STATE") && !initialSyncDone) {
            syncPiecesFromCsv(message.substring(12));
            initialSyncDone = true;
            return;
        } else if (message.startsWith("GAME_OVER")) {
            handleGameOver(message.substring(10));
            return;
        } else if (message.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, message, "Server says:", JOptionPane.ERROR_MESSAGE);

        }
    }

    private void applyMove(String move) {
        SwingUtilities.invokeLater(() -> {
            gameState.applyMove(move);
            refreshBoard();
        });
    }

    private void handleGameOver(String result) {
        SwingUtilities.invokeLater(() -> {
            setTitle("Game Over — " + result
                    + " — Client #" + client.getMyId());
            setEnabled(false);

        });
    }

    private int uiRow(int modelRow) {
        return playerColor == ChessGame.Color.WHITE ? modelRow : 7 - modelRow;
    }

    private int uiCol(int modelCol) {
        return playerColor == ChessGame.Color.WHITE ? modelCol : 7 - modelCol;
    }

    private int modelRow(int uiRow) {
        return playerColor == ChessGame.Color.WHITE ? uiRow : 7 - uiRow;
    }

    private int modelCol(int uiCol) {
        return playerColor == ChessGame.Color.WHITE ? uiCol : 7 - uiCol;
    }

    private void syncPiecesFromCsv(String csv) {
        String[] cells = csv.split(",");
        int idx = 0;

        // wipe every square first
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                gameState.setPiece(new ChessGame.Position(r, c), null);
            }
        }

        // refill according to the server dump
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++, idx++) {
                String tok = cells[idx];
                if ("--".equals(tok)) {
                    continue;
                }

                ChessGame.Color clr = tok.charAt(0) == 'w'
                        ? ChessGame.Color.WHITE : ChessGame.Color.BLACK;
                ChessGame.PieceType type = switch (tok.charAt(1)) {
                    case 'P' ->
                        ChessGame.PieceType.PAWN;
                    case 'R' ->
                        ChessGame.PieceType.ROOK;
                    case 'N' ->
                        ChessGame.PieceType.KNIGHT;
                    case 'B' ->
                        ChessGame.PieceType.BISHOP;
                    case 'Q' ->
                        ChessGame.PieceType.QUEEN;
                    case 'K' ->
                        ChessGame.PieceType.KING;
                    default ->
                        null;
                };
                gameState.setPiece(new ChessGame.Position(row, col),
                        new ChessGame.Piece(type, clr));
            }
        }
        refreshBoard();
    }

}
