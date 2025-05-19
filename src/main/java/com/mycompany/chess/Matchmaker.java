
package com.mycompany.chess;

import java.util.LinkedList;
import java.util.Queue;

public class Matchmaker {

    private final Queue<ClientHandler> waitingPlayers = new LinkedList<>();
    private final Object lock = new Object();

    public void addPlayer(ClientHandler player) {
        synchronized (lock) {

            if (waitingPlayers.contains(player)) {
                return;
            }
            waitingPlayers.add(player);
            System.out.println("Player added to queue. Queue size: " + waitingPlayers.size());

            if (waitingPlayers.size() >= 2) {
                ClientHandler player1 = waitingPlayers.poll();
                ClientHandler player2 = waitingPlayers.poll();

                if (validPlayers(player1, player2)) {
                    startNewGameSession(player1, player2);
                } else {
                    requeueValidPlayers(player1, player2);
                }
            }
        }
    }

    private boolean validPlayers(ClientHandler p1, ClientHandler p2) {
        return p1.isConnected() && p2.isConnected();
    }

    private void startNewGameSession(ClientHandler p1, ClientHandler p2) {
        try {
            waitingPlayers.remove(p1);
            waitingPlayers.remove(p2);
            GameSession session = new GameSession(p1, p2);
            p1.startGame(ChessGame.Color.WHITE, session);
            p2.startGame(ChessGame.Color.BLACK, session);
            session.start();
            System.out.println("Started new game session between "
                    + p1 + " and " + p2);
        } catch (Exception e) {
            System.err.println("Error creating game session: " + e.getMessage());
            safelyRequeuePlayer(p1);
            safelyRequeuePlayer(p2);
        }
    }

    private void safelyRequeuePlayer(ClientHandler player) {
        if (player != null && player.isConnected()) {
            synchronized (lock) {
                waitingPlayers.add(player);
            }
        }
    }

    private void requeueValidPlayers(ClientHandler p1, ClientHandler p2) {
        if (p1 != null && p1.isConnected()) {
            synchronized (lock) {
                waitingPlayers.add(p1);
            }
        }
        if (p2 != null && p2.isConnected()) {
            synchronized (lock) {
                waitingPlayers.add(p2);
            }
        }
    }

    public void removePlayer(ClientHandler player) {
        synchronized (lock) {
            waitingPlayers.remove(player);
            System.out.println("Player removed from queue. Queue size: " + waitingPlayers.size());
        }
    }

}
