package me.infuzion.web.server.response;

import me.infuzion.web.server.event.Event;

import java.net.Socket;

@FunctionalInterface
public interface ResponseGenerator {
    void generateResponse(Socket socket, Event event) throws Exception;
}
