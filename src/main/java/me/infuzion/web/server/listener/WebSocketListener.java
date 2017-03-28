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
import me.infuzion.web.server.event.EventHandler;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.PageRequestEvent;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

public class WebSocketListener implements EventListener {

    private final static String websocketGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public WebSocketListener(EventManager manager) {
        manager.registerListener(this);
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    @EventHandler
    public void onPageLoad(PageRequestEvent event) {
        final Map<String, String> headers = event.getHeaders();
        if (!headers.containsKey("Connection") || !headers.containsKey("Sec-WebSocket-Key")) {
            return;
        }
        if (!headers.get("Connection").equalsIgnoreCase("websocket")) {
            return;
        }

        String sha1;
        String websocketkey = headers.get("sec-websocket-key");
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update((websocketGUID + websocketkey).getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
            System.out.println(sha1);
            //TODO
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            event.setStatusCode(500);
            event.setHandled(true);
        }
    }

}
