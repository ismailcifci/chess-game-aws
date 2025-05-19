/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.chess;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class GameClient {

    private static final String SERVER_HOST = "16.171.40.243";
// or just "18.206.123.45" if you use the raw IPv4 address
    private static final int SERVER_PORT = 8888;

    private Runnable startCallback;
    private java.util.function.Consumer<String> endCallback;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GameScreen gameScreen;
    private boolean gameRunning = false;
    private int myId = -1;         // set later
    private HomeScreen home;

    public GameClient() {
        /* callbacks supplied once HomeScreen exists */ }

    public void connect() {
        System.out.println("connect() called");
        new Thread(this::readerLoop, "reader").start();
    }

    /* -------------------------------------------------- network loop */
    private void readerLoop() {
        try {
            System.out.println("readerLoop entered");
            socket = new Socket(SERVER_HOST, SERVER_PORT);          // or AWS IP
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connecting to " + SERVER_HOST + ":8888");

            String line;
            while ((line = in.readLine()) != null) {

                /* 1. first line that contains WHITE/BLACK -> create window */
                if (gameScreen == null) {
                    String colour = extractColour(line);
                    if (colour != null) {
                        SwingUtilities.invokeAndWait(() -> {
                            gameScreen = new GameScreen(colour, this, myId);
                            gameScreen.setVisible(true);
                        });
                        // don’t drop the START line –
                        // let the board also see it
                    }
                }

                /* 2. from here on every line goes to the board */
                final String msg = line;    // effectively final

                SwingUtilities.invokeLater(() -> {
                    if (gameScreen != null) {
                        gameScreen.handleServerMessage(msg);
                    }
                });

                if (msg.startsWith("GAME_START") && !gameRunning) {
                    gameRunning = true;
                    startCallback.run();           // hide “Searching…” dialog
                }

                if (msg.startsWith("GAME_OVER")) {
                    String result = msg.substring(10);      // e.g. "CHECKMATE WHITE"

                    /* freeze the board, but leave it visible */
                    if (gameScreen != null) {
                        String finalTitle = "Game Over — " + result;
                        SwingUtilities.invokeLater(() -> {
                            gameScreen.setTitle(finalTitle);
                            gameScreen.setEnabled(false);
                        });
                    }

                    /* open ONE translucent dialog via HomeScreen */
                    if (endCallback != null) // guard: callbacks now set
                    {
                        endCallback.accept(result);
                    }

                    gameRunning = false;        // allow JOIN later
                    /* board will be closed later by HomeScreen → client.closeBoardWindow() */
                    continue;
                }

                if (line.startsWith("CLIENT_ID")) {
                    myId = Integer.parseInt(line.substring(10).trim());

                    if (home == null) {
                        int idCopy = myId;                       // effectively final
                        SwingUtilities.invokeAndWait(() -> {
                            home = new HomeScreen(idCopy, this); // passes `this`
                            /* now we know the callbacks */
                            this.startCallback = home::onGameStart;
                            this.endCallback = home::onGameOver;
                        });
                    }
                    continue;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(()
                    -> JOptionPane.showMessageDialog(null, "Connection lost!", "Error",
                            JOptionPane.ERROR_MESSAGE));
        }
    }

    public void findNewGame() {
        if (out != null && !gameRunning) {   // socket ready, not in game
            out.println("JOIN");             // send exactly one request
        }
    }

    public void sendMove(String move) {
        if (out != null) {
            out.println("MOVE " + move);
        }
    }

    public BufferedReader getInputStream() {
        return this.in;
    }

    private String extractColour(String line) {
        if (line == null) {
            return null;
        }
        line = line.toUpperCase();

        if (line.contains("WHITE")) {
            return "WHITE";
        }
        if (line.contains("BLACK")) {
            return "BLACK";
        }
        return null;                   // not a colour message – ignore
    }

    public void notifyFinished(String result) {
        endCallback.accept(result);        // pass text to HomeScreen
    }

    public void closeBoardWindow() {
        if (gameScreen != null) {
            gameScreen.closeSilently();
            gameScreen = null;
        }
    }

    public int getMyId() {
        return myId;
    }

}
