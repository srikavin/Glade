/*
 * Copyright 2020 Srikavin Ramkumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.infuzion.web.server.listener;

import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.http.parser.BodyData;
import me.infuzion.web.server.http.parser.HttpRequest;
import me.infuzion.web.server.response.ResponseGenerator;
import me.infuzion.web.server.response.WebSocketResponseGenerator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketListenerTest {
    @Test
    void generateWebSocketAccept() {
        WebSocketListener a = new WebSocketListener();
        assertEquals(a.generateWebSocketAccept("dGhlIHNhbXBsZSBub25jZQ=="), "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=");
    }

    @Test
    void handlePageLoadNoWebsocket() {
        WebSocketListener listener = new WebSocketListener();

        HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                "/",
                "",
                "HTTP/1.1",
                Map.of(
                        "Host", "test"
                ),
                null
        );

        PageRequestEvent event = new PageRequestEvent(request, new BodyData(Collections.emptyMap()));
        ResponseGenerator def = event.getResponseGenerator();

        listener.onPageLoad(event);

        assertEquals(def, event.getResponseGenerator());
        assertEquals(0, event.getResponse().getHeaders().size());
    }

    @Test
    void handlePageLoadWebsocket() {
        WebSocketListener listener = new WebSocketListener();

        HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                "/",
                "",
                "HTTP/1.1",
                Map.of(
                        "Host", "test",
                        "Connection", "Upgrade",
                        "Upgrade", "websocket",
                        "Sec-WebSocket-Version", "13",
                        "Sec-WebSocket-Key", "al+GCxighZ3XA8lBd2SFZQ=="
                ),
                null
        );

        PageRequestEvent event = new PageRequestEvent(request, new BodyData(Collections.emptyMap()));
        ResponseGenerator def = event.getResponseGenerator();

        listener.onPageLoad(event);

        assertTrue(event.getResponseGenerator() instanceof WebSocketResponseGenerator);
        assertEquals("NwF8dAT89y2db1G8Zs84L4xjurY=", event.getResponse().getHeaders().get("sec-websocket-accept"));
        assertEquals("13", event.getResponse().getHeaders().get("sec-websocket-version"));
    }

}