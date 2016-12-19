package me.infuzion.web.server.listener;

import me.infuzion.web.server.EventManager;
import me.infuzion.web.server.PageLoadListener;
import me.infuzion.web.server.event.PageLoadEvent;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class WebSocketListener implements PageLoadListener {
    public final String websocketGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public WebSocketListener(EventManager manager){
        manager.registerListener(this);
    }

    @Override
    public void onPageLoad(PageLoadEvent event) {
        final Map<String, String> headers = event.getHeaders();
        if (!headers.containsKey("Connection") || !headers.containsKey("Sec-WebSocket-Key")) {
            return;
        }
        if (!headers.get("Connection").equalsIgnoreCase("websocket")) {
            return;
        }

        String websocketkey = headers.get("sec-websocket-key");
        MessageDigest cript = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update((websocketkey + websocketGUID).getBytes("utf-8"));
            byte[] digestBytes = digest.digest();
            String digestStr = javax.xml.bind.DatatypeConverter.printHexBinary(digestBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            event.setStatusCode(500);
            event.setHandled(true);
        }
    }
}
