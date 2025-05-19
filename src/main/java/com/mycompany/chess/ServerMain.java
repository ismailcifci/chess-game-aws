package com.mycompany.chess;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8888,
                50,
                java.net.InetAddress.getByName("0.0.0.0"));
        serverSocket.setReuseAddress(true);
        Matchmaker matchmaker = new Matchmaker();
        System.out.println("Server started on port 8888");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket, matchmaker).start();
        }
    }
}
