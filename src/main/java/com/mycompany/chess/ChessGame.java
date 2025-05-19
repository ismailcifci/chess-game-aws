
package com.mycompany.chess;

import java.util.ArrayList;
import java.util.List;

public class ChessGame {

    public enum Color {
        WHITE,
        BLACK;

        public Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    public enum PieceType {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    private Color currentTurn = Color.WHITE;
    private Piece[][] board = new Piece[8][8];
    private Position enPassantTarget = null;
    private boolean[] castlingRights = {true, true, true, true};
    private boolean gameOver = false;
    private Color winner = null;

    public static class Position {

        public final int row;
        public final int col;

        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public Position(String chessNotation) {
            this(8 - Character.getNumericValue(chessNotation.charAt(1)),
                    chessNotation.charAt(0) - 'a');
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Position other = (Position) obj;
            return row == other.row && col == other.col;
        }
    }

    public static class Piece {

        public final PieceType type;
        public final Color color;
        public boolean hasMoved;

        public Piece(PieceType type, Color color) {
            this.type = type;
            this.color = color;
            this.hasMoved = false;
        }
    }

    public ChessGame() {
        initializeBoard();
    }

    private void initializeBoard() {
        // Clear board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }

        // Pawns
        for (int col = 0; col < 8; col++) {
            board[6][col] = new Piece(PieceType.PAWN, Color.WHITE);   // white pawns
            board[1][col] = new Piece(PieceType.PAWN, Color.BLACK);   // black pawns
        }

        setupRow(7, Color.WHITE);   // white major pieces on rank 1
        setupRow(0, Color.BLACK);   // black major pieces on rank 8
    }

    private void setupRow(int row, Color color) {
        board[row][0] = new Piece(PieceType.ROOK, color);
        board[row][1] = new Piece(PieceType.KNIGHT, color);
        board[row][2] = new Piece(PieceType.BISHOP, color);
        board[row][3] = new Piece(PieceType.QUEEN, color);
        board[row][4] = new Piece(PieceType.KING, color);
        board[row][5] = new Piece(PieceType.BISHOP, color);
        board[row][6] = new Piece(PieceType.KNIGHT, color);
        board[row][7] = new Piece(PieceType.ROOK, color);
    }

    public boolean isValidMove(String moveStr, Color playerColor) {
        if (gameOver || playerColor != currentTurn) {
            return false;
        }

        try {
            Move move = parseMove(moveStr);
            return isValidMove(move);
        } catch (Exception e) {
            return false;
        }
    }

    private Move parseMove(String moveStr) {
        String[] parts = moveStr.split("-");
        Position from = new Position(parts[0]);
        Position to = new Position(parts[1]);
        String promotion = parts.length > 2 ? parts[2] : "";
        return new Move(from, to, promotion);
    }

    private class Move {

        public final Position from;
        public final Position to;
        public final String promotion;

        public Move(Position from, Position to, String promotion) {
            this.from = from;
            this.to = to;
            this.promotion = promotion;
        }
    }

    private boolean isValidMove(Move move) {
        Piece piece = getPiece(move.from);
        if (piece == null || piece.color != currentTurn) {
            return false;
        }

        // Check basic piece movement
        boolean valid = false;
        switch (piece.type) {
            case PAWN:
                valid = isValidPawnMove(move);
                break;
            case ROOK:
                valid = isValidRookMove(move);
                break;
            case KNIGHT:
                valid = isValidKnightMove(move);
                break;
            case BISHOP:
                valid = isValidBishopMove(move);
                break;
            case QUEEN:
                valid = isValidQueenMove(move);
                break;
            case KING:
                valid = isValidKingMove(move);
                break;
        }

        if (!valid) {
            return false;
        }

        // Check path is clear (except knights)
        if (piece.type != PieceType.KNIGHT && !isPathClear(move.from, move.to)) {
            return false;
        }

        // Check capture rules
        Piece targetPiece = getPiece(move.to);
        if (targetPiece != null && targetPiece.color == currentTurn) {
            return false;
        }

        // Check if leaves king in check
        if (wouldLeaveKingInCheck(move)) {
            return false;
        }

        return true;
    }

    private boolean isValidPawnMove(Move move) {
        Piece pawn = getPiece(move.from);
        int direction = pawn.color == Color.WHITE ? -1 : 1;
        int startRow = pawn.color == Color.WHITE ? 6 : 1;

        // Normal move
        if (move.from.col == move.to.col) {
            if (move.to.row == move.from.row + direction && getPiece(move.to) == null) {
                return true;
            }
            if (move.from.row == startRow && move.to.row == move.from.row + 2 * direction
                    && getPiece(move.to) == null && getPiece(new Position(move.from.row + direction, move.from.col)) == null) {
                return true;
            }
        }

        // Capture
        if (Math.abs(move.from.col - move.to.col) == 1
                && move.to.row == move.from.row + direction) {

            // Regular capture
            if (getPiece(move.to) != null) {
                return true;
            }

            // En passant
            if (enPassantTarget != null && move.to.equals(enPassantTarget)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidKnightMove(Move move) {
        int rowDiff = Math.abs(move.from.row - move.to.row);
        int colDiff = Math.abs(move.from.col - move.to.col);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(Move move) {
        return Math.abs(move.from.row - move.to.row) == Math.abs(move.from.col - move.to.col);
    }

    private boolean isValidRookMove(Move move) {
        return move.from.row == move.to.row || move.from.col == move.to.col;
    }

    private boolean isValidQueenMove(Move move) {
        return isValidBishopMove(move) || isValidRookMove(move);
    }

    private boolean isValidKingMove(Move move) {
        // Normal move
        if (Math.abs(move.from.row - move.to.row) <= 1
                && Math.abs(move.from.col - move.to.col) <= 1) {
            return true;
        }

        // Castling
        return isValidCastlingMove(move);
    }

    private boolean isValidCastlingMove(Move move) {
        if (getPiece(move.from).hasMoved) {
            return false;
        }

        int row = move.from.row;
        int colDiff = move.to.col - move.from.col;

        if (Math.abs(colDiff) != 2) {
            return false;
        }

        int rookCol = colDiff > 0 ? 7 : 0;
        Piece rook = getPiece(new Position(row, rookCol));
        if (rook == null || rook.type != PieceType.ROOK || rook.hasMoved) {
            return false;
        }

        // Check path is clear and not under attack
        int step = colDiff > 0 ? 1 : -1;
        for (int c = move.from.col + step; c != rookCol; c += step) {
            if (getPiece(new Position(row, c)) != null) {
                return false;
            }
        }

        return !isSquareAttacked(new Position(row, move.from.col + step), currentTurn.opposite());
    }

    private boolean isPathClear(Position from, Position to) {
        int rowStep = Integer.compare(to.row - from.row, 0);
        int colStep = Integer.compare(to.col - from.col, 0);

        Position current = new Position(from.row + rowStep, from.col + colStep);
        while (!current.equals(to)) {
            if (getPiece(current) != null) {
                return false;
            }
            current = new Position(current.row + rowStep, current.col + colStep);
        }
        return true;
    }

    private boolean wouldLeaveKingInCheck(Move move) {
        // Simulate move
        Piece original = getPiece(move.to);
        setPiece(move.to, getPiece(move.from));
        setPiece(move.from, null);

        boolean inCheck = isKingInCheck(currentTurn);

        // Revert move
        setPiece(move.from, getPiece(move.to));
        setPiece(move.to, original);

        return inCheck;
    }

    public boolean isKingInCheck(Color color) {
        Position kingPos = findKing(color);
        return isSquareAttacked(kingPos, color.opposite());
    }

    private Position findKing(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = getPiece(new Position(row, col));
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }

    private boolean isSquareAttacked(Position target, Color attackerColor) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                Piece piece = getPiece(from);
                if (piece != null && piece.color == attackerColor) {
                    Move testMove = new Move(from, target, "");
                    if (isValidMoveForPiece(testMove, piece)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidMoveForPiece(Move move, Piece piece) {
        switch (piece.type) {

            /* ---------- pawn diagonal capture only ------------------- */
            case PAWN:
                int dir = (piece.color == Color.WHITE ? -1 : 1);
                return (move.to.col == move.from.col + 1
                        || move.to.col == move.from.col - 1)
                        && move.to.row == move.from.row + dir;

            /* ---------- rook, bishop, queen need an empty ray -------- */
            case ROOK:
                return isValidRookMove(move) && isPathClear(move.from, move.to);

            case BISHOP:
                return isValidBishopMove(move) && isPathClear(move.from, move.to);

            case QUEEN:
                return isValidQueenMove(move) && isPathClear(move.from, move.to);

            /* ---------- knight jumps, king moves one square ---------- */
            case KNIGHT:
                return isValidKnightMove(move);

            case KING:
                return isValidKingMove(move);

            default:
                return false;
        }
    }

    public void applyMove(String moveStr) {
        Move move = parseMove(moveStr);
        Piece piece = getPiece(move.from);
        if (piece == null) {
            return;
        }

        /* 1.  clear any old en-passant opportunity ------------------- */
        enPassantTarget = null;          // ← NEW LINE

        
        PieceType promoType = null;
        if (!move.promotion.isEmpty()) {
            /* accept both “Q” and “QUEEN”, “R” / “ROOK”, … */
            switch (move.promotion.toUpperCase()) {
                case "Q", "QUEEN" ->
                    promoType = PieceType.QUEEN;
                case "R", "ROOK" ->
                    promoType = PieceType.ROOK;
                case "B", "BISHOP" ->
                    promoType = PieceType.BISHOP;
                case "N", "KNIGHT" ->
                    promoType = PieceType.KNIGHT;
                default -> {
                    /* ignore invalid string */ }
            }
        }

        // Handle special moves
        if (piece.type == PieceType.PAWN) {
            handlePawnSpecial(move);
        } else if (piece.type == PieceType.KING && Math.abs(move.from.col - move.to.col) == 2) {
            handleCastling(move);
        }

        // Update board state
        setPiece(move.to, piece);
        setPiece(move.from, null);
        piece.hasMoved = true;

        if (piece.type == PieceType.PAWN
                && (move.to.row == 0 || move.to.row == 7)) {
            if (promoType == null) {
                promoType = PieceType.QUEEN;  // default
            }
            setPiece(move.to, new Piece(promoType, piece.color));
        }

        // Update game state
        currentTurn = currentTurn.opposite();
        checkGameOver();
    }

    private void handlePawnSpecial(Move move) {
        // En passant
        if (move.to.equals(enPassantTarget)) {
            Position capturedPawn = new Position(move.from.row, move.to.col);
            setPiece(capturedPawn, null);
        }

        // Set en passant target
        if (Math.abs(move.from.row - move.to.row) == 2) {
            enPassantTarget = new Position((move.from.row + move.to.row) / 2, move.from.col);
        }
    }

    private void handleCastling(Move move) {
        int rookCol = move.to.col > move.from.col ? 7 : 0;
        int newRookCol = move.to.col > move.from.col ? 5 : 3;
        Piece rook = getPiece(new Position(move.from.row, rookCol));
        setPiece(new Position(move.from.row, newRookCol), rook);
        setPiece(new Position(move.from.row, rookCol), null);
    }

    private void checkGameOver() {
        /* The side *to move* (currentTurn) is the one that could be      *
     * check-mated or stalemated.                                     */
        if (isCheckmate(currentTurn)) {
            gameOver = true;
            /* The *other* player delivered the mate.                     */
            winner = currentTurn.opposite();
        } else if (isStalemate()) {
            gameOver = true;        // stalemate → draw
            winner = null;
        }
    }

    private boolean isCheckmate(Color color) {
        return isKingInCheck(color) && hasNoLegalMoves(color);
    }

    private boolean isStalemate() {
        return !isKingInCheck(currentTurn) && hasNoLegalMoves(currentTurn);
    }

    private boolean hasNoLegalMoves(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position pos = new Position(row, col);
                Piece piece = getPiece(pos);
                if (piece != null && piece.color == color) {
                    List<Position> moves = calculateLegalMoves(pos);
                    if (!moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private List<Position> calculateLegalMoves(Position from) {
        List<Position> moves = new ArrayList<>();
        ChessGame.Piece piece = getPiece(from);

        if (piece == null) {
            return moves;
        }

        Color realTurn = currentTurn;
        currentTurn = piece.color;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (row == from.row && col == from.col) {
                    continue;   // skip same square
                }
                Position to = new Position(row, col);
                if (isValidMove(new Move(from, to, ""))) {
                    moves.add(to);
                }
            }
        }

        currentTurn = realTurn;
        return moves;
    }

    public Piece getPiece(Position pos) {
        return board[pos.row][pos.col];
    }

    public void setPiece(Position pos, Piece piece) {
        board[pos.row][pos.col] = piece;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Color getWinner() {
        return winner;
    }

    public List<Position> getLegalMoves(Position from) {
        return calculateLegalMoves(from);
    }
}
