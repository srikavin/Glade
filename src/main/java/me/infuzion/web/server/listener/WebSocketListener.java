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

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventControl;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.EventPriority;
import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.http.HttpResponse;
import me.infuzion.web.server.network.websocket.WebsocketConnectionHandler;
import me.infuzion.web.server.response.ResponseGenerator;
import me.infuzion.web.server.response.WebSocketResponseGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

//TODO: Update
public class WebSocketListener implements EventListener {
    private final static String websocketGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final static Base64.Encoder b64Encoder;
    private final static MessageDigest sha1Digest;

    static {
        try {
            b64Encoder = Base64.getEncoder();
            sha1Digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final ResponseGenerator websocketResponseGenerator = new WebSocketResponseGenerator();

    @EventHandler(priority = EventPriority.START, control = EventControl.FULL)
    public boolean onPageLoad(PageRequestEvent event) {
        final Map<String, String> headers = event.getHeaders();

        if (!(event.getHttpMethod() == HttpMethod.GET)) {
            return false;
        }

        if (!headers.containsKey("Connection") || !headers.containsKey("Upgrade") || !headers.containsKey("Sec-WebSocket-Key")) {
            return false;
        }

        String[] split = headers.get("Connection").split(",");
        boolean found = false;
        for (String e : split) {
            if (e.trim().equalsIgnoreCase("upgrade")) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }

        if (!headers.get("Upgrade").equalsIgnoreCase("websocket")) {
            return false;
        }

        String webSocketKey = headers.get("sec-websocket-key");

        HttpResponse response = event.getResponse();
        response.setHeader("Sec-WebSocket-Accept", generateWebSocketAccept(webSocketKey));
        response.setHeader("Sec-WebSocket-Version", "13");

        event.setResponseGenerator(websocketResponseGenerator);
        event.setConnectionHandler(WebsocketConnectionHandler.class);

        return true;
    }

    public String generateWebSocketAccept(String secKey) {
        return b64Encoder.encodeToString(sha1Digest.digest((secKey + websocketGUID).getBytes(StandardCharsets.UTF_8)));
    }
}
