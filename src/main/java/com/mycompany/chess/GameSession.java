
package com.mycompany.chess;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class GameSession extends Thread {

    private final ClientHandler whitePlayer;
    private final ClientHandler blackPlayer;
    private final ChessGame game;
    private final ReentrantLock gameLock = new ReentrantLock();
    private boolean gameActive = true;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.whitePlayer = player1;
        this.blackPlayer = player2;
        this.game = new ChessGame();

        player1.setCurrentSession(this);
        player2.setCurrentSession(this);
    }

    public ChessGame.Color getCurrentTurn() {
        return game.getCurrentTurn();
    }

    public String getBoardState() {
        return serializeBoardState();
    }

    @Override
    public void run() {
        // Notify players about game start
        whitePlayer.sendMessage("GAME_START WHITE");
        blackPlayer.sendMessage("GAME_START BLACK");
        // Send initial board state
        sendBoardState();
        while (gameActive) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            endGameCleanup();
        } catch (IOException ignored) {
        }
    }

    public void processMove(ClientHandler player, String move) {
        gameLock.lock();
        try {
            ChessGame.Color playerColor = (player == whitePlayer)
                    ? ChessGame.Color.WHITE : ChessGame.Color.BLACK;

            if (game.isValidMove(move, playerColor)) {
                // Apply move to game state
                game.applyMove(move);

                // Broadcast move to both players
                broadcastMove(move);

                // Check game status
                checkGameStatus();
            } else {
                player.sendMessage("ERROR Invalid move: " + move);
            }
        } finally {
            gameLock.unlock();
        }
    }

    private void broadcastMove(String move) {
        whitePlayer.sendMessage("MOVE " + move);
        blackPlayer.sendMessage("MOVE " + move);
    }

    private void sendBoardState() {
        String boardState = serializeBoardState();
        whitePlayer.sendMessage("BOARD_STATE " + boardState);
        blackPlayer.sendMessage("BOARD_STATE " + boardState);
    }

    private String serializeBoardState() {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessGame.Piece p = game.getPiece(new ChessGame.Position(row, col));

                if (p != null) {
                    sb.append(p.color == ChessGame.Color.WHITE ? "w" : "b");
                    sb.append(pieceLetter(p.type)); 
                } else {
                    sb.append("--");
                }
                sb.append(',');
            }
        }
        return sb.toString();
    }

    private char pieceLetter(ChessGame.PieceType t) {
        return switch (t) {
            case PAWN ->
                'P';
            case ROOK ->
                'R';
            case KNIGHT ->
                'N';   // <-- distinct from King
            case BISHOP ->
                'B';
            case QUEEN ->
                'Q';
            case KING ->
                'K';
        };
    }

    private void checkGameStatus() {
        if (game.isGameOver()) {
            String result;
            if (game.getWinner() != null) {
                result = "CHECKMATE " + game.getWinner().name();
            } else {
                result = "STALEMATE";
            }
            whitePlayer.sendMessage("GAME_OVER " + result);
            blackPlayer.sendMessage("GAME_OVER " + result);

            whitePlayer.setCurrentSession(null);
            blackPlayer.setCurrentSession(null);
            gameActive = false;
        }
    }

    private void endGameCleanup() throws IOException {
        gameActive = false;

    }

    public void handleDisconnection(ClientHandler disconnectedPlayer) {
        gameLock.lock();
        try {
            if (gameActive) {
                ClientHandler remainingPlayer = (disconnectedPlayer == whitePlayer)
                        ? blackPlayer : whitePlayer;

                if (remainingPlayer.isConnected()) {
                    remainingPlayer.sendMessage("GAME_OVER OPPONENT_DISCONNECTED");
                    remainingPlayer.setCurrentSession(null);
                }
                gameActive = false;
            }
        } finally {
            gameLock.unlock();
        }
    }
}
