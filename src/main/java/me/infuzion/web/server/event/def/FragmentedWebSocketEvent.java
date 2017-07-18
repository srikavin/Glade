package me.infuzion.web.server.event.def;

import java.util.UUID;

public class FragmentedWebSocketEvent extends WebSocketEvent {

    public FragmentedWebSocketEvent(UUID sessionUUID, String payload, byte payloadOpCode, PageRequestEvent event) {
        super(sessionUUID, payload, payloadOpCode, false, event);
    }
}
