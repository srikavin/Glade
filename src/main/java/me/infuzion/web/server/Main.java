package me.infuzion.web.server;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(90);
        Server server = new Server(socket);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
