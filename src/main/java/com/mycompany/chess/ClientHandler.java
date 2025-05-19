
package com.mycompany.chess;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Matchmaker matchmaker;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);
    private int clientId = 0; 

    private GameSession gameSession;
    private ChessGame.Color playerColor;

    public ClientHandler(Socket socket, Matchmaker matchmaker) throws IOException {
        this.clientSocket = socket;
        this.matchmaker = matchmaker;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.clientId = ID_GEN.getAndIncrement();
        sendMessage("CLIENT_ID " + clientId);  
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                String input = in.readLine();
                if (input == null) {
                    break;
                }

                handleInput(input);
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleInput(String input) {
        if (input.startsWith("JOIN")) {
            handleJoin();
        } else if (input.startsWith("MOVE")) {
            handleMove(input.substring(5));
        }
    }

    private void handleJoin() {
        matchmaker.addPlayer(this);
    }

    private void handleMove(String move) {
        if (gameSession != null && playerColor == gameSession.getCurrentTurn()) {
            gameSession.processMove(this, move);
        } else {
            sendMessage("ERROR Not your turn");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void startGame(ChessGame.Color color, GameSession session) {
        this.gameSession = session;
        this.playerColor = color;
        sendMessage("GAME_START " + color);
        sendCurrentBoardState();
    }

    private void sendCurrentBoardState() {
        if (gameSession != null) {
            sendMessage("BOARD_STATE " + gameSession.getBoardState());
        }
    }

    public void disconnect() {
        matchmaker.removePlayer(this);
        running.set(false);
        try {
            if (gameSession != null) {
                gameSession.handleDisconnection(this);
            }
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed();
    }

    // Getters
    public ChessGame.Color getPlayerColor() {
        return playerColor;
    }

    public ChessGame.Color getCurrentTurn() {
        return gameSession != null ? gameSession.getCurrentTurn() : null;
    }

    public void processMove(String move) {
        if (gameSession != null) {
            gameSession.processMove(this, move);
        }
    }

    public String getBoardState() {
        return gameSession != null ? gameSession.getBoardState() : "";
    }

    public void setCurrentSession(GameSession session) {
        this.gameSession = session;
    }

    public String waitForMove() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }

    }
}
