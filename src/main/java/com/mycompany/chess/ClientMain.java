
package com.mycompany.chess;

public class ClientMain {

    public static void main(String[] args) {
        System.out.println("main entered");
        GameClient gc = new GameClient();                       
        System.out.println("GameClient constructed");
        gc.connect();
        System.out.println("connect() returned, main ends");
    }
}
