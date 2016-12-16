package me.infuzion.web.server;

import me.infuzion.web.server.event.PageLoadEvent;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private final double version = 0.1;

    private boolean running;
    private ServerSocket serverSocket;
    private EventManager eventManager;
    private long lastRequestTime = -1;

    public Server(ServerSocket socket) {
        this.serverSocket = socket;
        running = true;
        eventManager = new EventManager();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                lastRequestTime = System.currentTimeMillis();
                onRequest(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onRequest(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter writer = new PrintWriter(client.getOutputStream());

        int contentLength = -1;
        String page = "";
        while (true) {
            final String line = reader.readLine();
            if(line == null){
                return;
            }
            final String contentLengthStr = "Content-Length: ";
            if (line.startsWith(contentLengthStr)) {
                contentLength = Integer.parseInt(line.substring(contentLengthStr.length()));
                continue;
            }

            if (line.startsWith("GET") || line.startsWith("POST")) {
                page = line.split(" ")[1];
                continue;
            }

            if (line.length() == 0) {
                break;
            }
        }
        String contentString = null;
        if (contentLength != -1 && contentLength >= 0) {
            final char[] content = new char[contentLength];
            if (reader.read(content) == -1) {
                return;
            }
            contentString = new String(content);
        }

        PageLoadEvent event = new PageLoadEvent(page, contentString);
        eventManager.callEvent(event);
        System.out.println("Request recieved from: " + client.getInetAddress() + ":" + client.getPort() + " - " +
                event.getStatusCode() + " " + (System.currentTimeMillis() - lastRequestTime) + "ms " + event.getPage());
        generateResponse(writer, event.getStatusCode(), event.getFileEncoding(), event.getResponseData());
    }

    private void generateResponse(PrintWriter writer, int status, String contentType, String responseData)
            throws UnsupportedEncodingException {
        writer.println("HTTP/1.1 " + status + "\r");
        writer.println("Content-Type: " + contentType + "; charset=UTF-8\r");
        writer.println("Content-Length: " + responseData.getBytes("UTF-8").length + "\r");
        writer.println("X-Powered-By: Java Web Server v" + version + "\r");
        writer.println("X-Request-Time: " + (System.currentTimeMillis() - lastRequestTime) + "\r\n");
        writer.println(responseData);
        writer.flush();
    }
}
