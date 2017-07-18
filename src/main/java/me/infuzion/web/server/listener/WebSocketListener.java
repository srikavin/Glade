/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.listener;

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventControl;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.EventPriority;
import me.infuzion.web.server.response.WebSocketResponseGenerator;
import me.infuzion.web.server.util.HTTPMethod;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class WebSocketListener implements EventListener {

    private final static String websocketGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private final static HashMap<String, WebSocketResponseGenerator> urlResponseGeneratorMap = new HashMap<>();
    private EventManager manager;

    public WebSocketListener(EventManager manager) {
        this.manager = manager;
        manager.registerListener(this);
    }

    @EventHandler(priority = EventPriority.START, control = EventControl.FULL)
    public boolean onPageLoad(PageRequestEvent event) {
        final Map<String, String> headers = event.getHeaders();
        if (!(event.getMethod() == HTTPMethod.GET)) {
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
        try {
            event.addHeader("Sec-WebSocket-Accept", generateWebSocketAccept(webSocketKey));
            event.addHeader("Sec-WebSocket-Version", "13");
            urlResponseGeneratorMap.computeIfAbsent(event.getPage(), (url) -> {
                WebSocketResponseGenerator responseGenerator = new WebSocketResponseGenerator(manager);
                Thread thread = new Thread(responseGenerator);
                thread.setName("Websocket listener for " + event.getPage());
                thread.start();
                return responseGenerator;
            });
            event.setResponseGenerator(urlResponseGeneratorMap.get(event.getPage()));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            event.setStatusCode(500);
            event.setHandled(true);
            return false;
        }
        return true;
    }

    public String generateWebSocketAccept(String secKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return DatatypeConverter.printBase64Binary(
                MessageDigest.getInstance("SHA-1")
                        .digest((secKey + websocketGUID)
                                .getBytes("UTF-8")));

    }
}
