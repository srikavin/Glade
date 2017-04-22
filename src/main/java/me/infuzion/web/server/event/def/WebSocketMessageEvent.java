package me.infuzion.web.server.event.def;

import java.util.UUID;

public class WebSocketMessageEvent extends WebSocketEvent {
    public WebSocketMessageEvent(UUID sessionUUID, String payload, byte payloadOpCode, String page) {
        super(sessionUUID, payload, payloadOpCode, true, page);
    }
}
