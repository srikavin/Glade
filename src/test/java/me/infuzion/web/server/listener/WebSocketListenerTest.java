package me.infuzion.web.server.listener;

import me.infuzion.web.server.event.EventManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketListenerTest {
    @Test
    void generateWebSocketAccept() throws Exception {
        WebSocketListener a = new WebSocketListener(new EventManager());
        assertEquals(a.generateWebSocketAccept("dGhlIHNhbXBsZSBub25jZQ=="), "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=");
    }

}