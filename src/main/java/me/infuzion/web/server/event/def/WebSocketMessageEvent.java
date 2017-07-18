package me.infuzion.web.server.event.def;

import java.util.UUID;

public class WebSocketMessageEvent extends WebSocketEvent {
    public WebSocketMessageEvent(UUID sessionUUID, String payload, byte payloadOpCode, PageRequestEvent event) {
        super(sessionUUID, payload, payloadOpCode, true, event);
    }
}
