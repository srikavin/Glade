package me.infuzion.web.server.response;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.FragmentedWebSocketEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.def.WebSocketEvent;
import me.infuzion.web.server.event.def.WebSocketMessageEvent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebSocketResponseGenerator extends DefaultResponseGenerator implements Runnable {
    private final static int MAX_SIZE = 65536;
    private final static int PING_TICKS = 20;
    private final Thread thread;
    private final ArrayList<WebSocketClient> connections;
    private String page;
    private EventManager manager;

    public WebSocketResponseGenerator(EventManager manager) {
        this.manager = manager;
        connections = new ArrayList<>();
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void generateResponse(Socket socket, Event event) throws Exception {
        if (!(event instanceof PageRequestEvent)) {
            return;
        }
        page = ((PageRequestEvent) event).getPage();
        thread.setName("WebSocket Thread for '" + page + "'");
        Writer writer = getWriterFromSocket(socket);
        writer.write("HTTP/1.1 101 Switching Protocols\r\n");
        writeHeaderLine(writer, "Upgrade", "websocket");
        writeHeaderLine(writer, "Connection", "Upgrade");
        writeHeaders(writer, ((PageRequestEvent) event).getAdditionalHeadersToSend());
        writeLastHeaderLine(writer, "WebSocket", "WebSocket");
        writer.flush();
        synchronized (connections) {
            connections.add(new WebSocketClient(socket, ((PageRequestEvent) event).getSessionUuid()));
        }
    }

    public void sendTextFrame(String payload) throws IOException {
        sendFrameToAll(payload, (byte) 0x1);
    }

    public void sendFrame(UUID id, String payload, byte opCode) throws IOException {
        for (WebSocketClient e : connections) {
            if (e.getSessionUUID().equals(id)) {
                sendFrame(e.getSocket(), generateFrame(payload, opCode));
                return;
            }
        }
    }

    protected void sendFrame(Socket e, byte[] payload) throws IOException {
        e.getOutputStream().write(payload);
        e.getOutputStream().flush();
    }

    protected byte[] generateFrame(String payload, byte opCode) throws UnsupportedEncodingException {
        byte[] payloadBytes = payload.getBytes("UTF-8");
        byte[] toSend = new byte[128 + payloadBytes.length];

        toSend[0] = (byte) (0b1000_0000 + opCode);

        short dataStartIndex;

        if (payloadBytes.length < 125) {
            toSend[1] = (byte) payloadBytes.length;
            dataStartIndex = 2;
        } else if (payloadBytes.length >= 256 && payloadBytes.length < 65535) {
            toSend[1] = 126;
            toSend[2] = (byte) ((payloadBytes.length >> 8) & 255);
            toSend[3] = (byte) (payloadBytes.length & 255);
            dataStartIndex = 4;
        } else {
            toSend[1] = 127;
            toSend[2] = (byte) ((payloadBytes.length >> 24) & 255);
            toSend[3] = (byte) ((payloadBytes.length >> 16) & 255);
            toSend[4] = (byte) ((payloadBytes.length >> 8) & 255);
            toSend[5] = (byte) (payloadBytes.length & 255);
            toSend[6] = (byte) ((payloadBytes.length >> 24) & 255);
            toSend[7] = (byte) ((payloadBytes.length >> 16) & 255);
            toSend[8] = (byte) ((payloadBytes.length >> 8) & 255);
            toSend[9] = (byte) (payloadBytes.length & 255);
            dataStartIndex = 10;
        }

        System.arraycopy(payloadBytes, 0, toSend, dataStartIndex, payloadBytes.length);
        return toSend;
    }

    protected void sendFrameToAll(String payload, byte opCode) throws IOException {
        byte[] toSend = generateFrame(payload, opCode);
        synchronized (connections) {
            for (WebSocketClient e : connections) {
                sendFrame(e.getSocket(), toSend);
            }
        }
    }

    @Override
    public void run() {
        ArrayList<WebSocketClient> toRemove = new ArrayList<>();
        int counter = 0;
        while (true) {
            try {
                synchronized (connections) {
                    counter++;
                    for (WebSocketClient e : connections) {
                        try {
                            if (counter % 20 == 0) {
                                sendFrame(e.getSocket(), generateFrame("ping", (byte) 0x9));
                            }

                            BufferedInputStream inputStream = new BufferedInputStream(e.getSocket().getInputStream());
                            if (inputStream.available() > 0) {
                                byte[] headers = new byte[2]; // Websocket headers
                                int size = inputStream.read(headers);

                                if (size != 2) {
                                    toRemove.add(e);
                                    continue;
                                }

                                boolean fin = ((headers[0] >> 7) & 1) != 0;
                                boolean masked = ((headers[1] >> 7) & 1) == 1;

                                if (!masked) {
                                    toRemove.add(e); // Client must mask connections to server
                                    continue;
                                }

                                long payloadSize = headers[1] & 127;
                                byte opCode = (byte) (headers[0] - ((headers[0] >> 4) << 4));

                                if (payloadSize == 126) {
                                    byte[] len = new byte[2];
                                    int read = inputStream.read(len);
                                    if (read != 2) {
                                        toRemove.add(e);
                                        continue;
                                    }
                                    ByteBuffer buffer = ByteBuffer.wrap(len);
                                    payloadSize = buffer.getShort(0);
                                } else if (payloadSize == 127) { // too big
                                    continue;
                                    /*byte[] len = new byte[8];
                                    int read = inputStream.read(len);
                                    if (read != 8) {
                                        toRemove.add(e);
                                        continue;
                                    }
                                    ByteBuffer buffer = ByteBuffer.wrap(len);
                                    payloadSize = buffer.getShort(0);*/
                                }

                                byte[] masks = new byte[4];
                                int masksRead = inputStream.read(masks);

                                if (masksRead != 4) {
                                    toRemove.add(e);
                                    continue;
                                }

                                byte[] decoded = new byte[(int) payloadSize];
                                byte[] payload = new byte[(int) payloadSize];
                                inputStream.read(payload);

                                for (int i = 0; i < payloadSize; i++) {
                                    decoded[i] = (byte) (payload[i] ^ masks[i % 4]);
                                }

                                String decodedPayload = new String(decoded, "UTF-8");
                                e.lastPing = System.currentTimeMillis();

                                if (opCode == 0xA) {
                                    continue;
                                }


                                if (opCode == 0x9) {
                                    sendFrame(e.getSocket(), generateFrame(decodedPayload, (byte) 0x10));
                                    continue;
                                }

                                WebSocketEvent event;
                                if (fin) {
                                    if (e.lastPartialMessage.equals("")) {
                                        event = new WebSocketMessageEvent(e.getSessionUUID(), decodedPayload, opCode, page);
                                    } else {
                                        event = new WebSocketMessageEvent(e.getSessionUUID(),
                                                e.lastPartialMessage + decodedPayload, opCode, page);
                                        System.out.println(e.lastPartialMessage + decodedPayload);
                                    }
                                    e.lastPartialMessage = "";
                                } else {
                                    e.lastPartialMessage += decodedPayload;
                                    event = new FragmentedWebSocketEvent(e.getSessionUUID(), decodedPayload, opCode, page);
                                }
                                manager.fireEvent(event);

                                for (String toAll : event.getToSendToAll()) {
                                    byte[] toSend = generateFrame(toAll, (byte) 0x1);
                                    for (WebSocketClient client : connections) {
                                        try {
                                            sendFrame(client.getSocket(), toSend);
                                        } catch (Exception ex) {
                                            toRemove.add(client);
                                        }
                                    }
                                }

                                for (Map.Entry<UUID, String> entry : event.getToSend().entrySet()) {
                                    sendFrame(entry.getKey(), entry.getValue(), (byte) 0x1);
                                }
                            }
                        } catch (IOException ex) {
                            toRemove.add(e);
                        }
                    }
                    long currentTime = System.currentTimeMillis();
                    for (WebSocketClient e : connections) {
                        if (currentTime - e.lastPing > 2000) {
                            toRemove.add(e);
                        }
                    }
                    for (WebSocketClient e : toRemove) {
                        e.getSocket().close();
                        connections.remove(e);
                    }
                    toRemove.clear();
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    private static class WebSocketClient {
        private final Socket socket;
        private final UUID sessionUUID;
        long lastPing;
        String lastPartialMessage = "";

        public WebSocketClient(Socket socket, UUID sessionUUID) {
            this.socket = socket;
            this.sessionUUID = sessionUUID;
            this.lastPing = System.currentTimeMillis();
        }

        Socket getSocket() {
            return socket;
        }

        public UUID getSessionUUID() {
            return sessionUUID;
        }
    }
}
