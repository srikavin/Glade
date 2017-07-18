package me.infuzion.web.server.response;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.FragmentedWebSocketEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.def.WebSocketEvent;
import me.infuzion.web.server.event.def.WebSocketMessageEvent;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class WebSocketResponseGenerator extends DefaultResponseGenerator implements Runnable {
    private final static int MAX_SIZE = 65536;
    private final static int PING_TICKS = 20;
    private final ArrayList<WebSocketClient> connections;
    private EventManager manager;

    public WebSocketResponseGenerator(EventManager manager) {
        this.manager = manager;
        connections = new ArrayList<>();
    }

    @SuppressWarnings("resource")
    @Override
    public void generateResponse(Socket socket, Event event) throws Exception {
        if (!(event instanceof PageRequestEvent)) {
            return;
        }
        PageRequestEvent pEvent = (PageRequestEvent) event;
        Writer writer = getWriterFromSocket(socket);
        writer.write("HTTP/1.1 101 Switching Protocols\r\n");
        writeHeaderLine(writer, "Upgrade", "websocket");
        writeHeaderLine(writer, "Connection", "Upgrade");
        writeHeaders(writer, ((PageRequestEvent) event).getAdditionalHeadersToSend());
        writeLastHeaderLine(writer, "WebSocket", "WebSocket");
        writer.flush();
        synchronized (connections) {
            connections.add(new WebSocketClient(socket, pEvent));
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
        OutputStream stream = e.getOutputStream();
        stream.write(payload);
        stream.flush();
    }

    protected byte[] generateFrame(String payload, byte opCode) throws UnsupportedEncodingException {
        System.out.println("Sending " + payload + " with opCode: " + opCode);
        byte[] payloadBytes = payload.getBytes("UTF-8");
        byte[] header = new byte[10];

        header[0] = (byte) (0b1000_0000 + opCode);

        short dataStartIndex;

        if (payloadBytes.length <= 125) {
            header[1] = (byte) payloadBytes.length;
            dataStartIndex = 2;
        } else if (payloadBytes.length <= 65535) {
            header[1] = 126;
            header[2] = (byte) ((payloadBytes.length >> 8) & 255);
            header[3] = (byte) (payloadBytes.length & 255);
            dataStartIndex = 4;
        } else {
            header[1] = 127;
            header[2] = (byte) ((payloadBytes.length >> 24) & 255);
            header[3] = (byte) ((payloadBytes.length >> 16) & 255);
            header[4] = (byte) ((payloadBytes.length >> 8) & 255);
            header[5] = (byte) (payloadBytes.length & 255);
            header[6] = (byte) ((payloadBytes.length >> 24) & 255);
            header[7] = (byte) ((payloadBytes.length >> 16) & 255);
            header[8] = (byte) ((payloadBytes.length >> 8) & 255);
            header[9] = (byte) (payloadBytes.length & 255);
            dataStartIndex = 10;
        }
        byte[] toRet = new byte[dataStartIndex + payloadBytes.length];
        System.arraycopy(header, 0, toRet, 0, dataStartIndex);
        System.arraycopy(payloadBytes, 0, toRet, dataStartIndex, payloadBytes.length);
        System.out.println(Arrays.toString(toRet));
        return toRet;
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
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                synchronized (connections) {
                    counter++;
                    for (WebSocketClient e : connections) {
                        try {
                            if (counter % 20 == 0) {
                                sendFrame(e.getSocket(), generateFrame("ping", (byte) 0x9));
                            }

                            //noinspection IOResourceOpenedButNotSafelyClosed, resource
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
                                int readSize = inputStream.read(payload);
                                if (payloadSize != readSize) {
                                    System.out.println("readSize != payloadSize");
                                }

                                for (int i = 0; i < payloadSize; i++) {
                                    decoded[i] = (byte) (payload[i] ^ masks[i % 4]);
                                }


                                String decodedPayload = new String(decoded, "UTF-8");
                                e.lastPing = System.currentTimeMillis();

                                if (opCode == 0x8) {
                                    toRemove.add(e);
                                    continue;
                                }
                                if (opCode == 0x9) {
                                    sendFrame(e.getSocket(), generateFrame(decodedPayload, (byte) 0x10));
                                    continue;
                                }

                                if (opCode == 0xA) {
                                    continue;
                                }
                                WebSocketEvent event;
                                if (fin) {
                                    if (e.lastPartialMessage == null) {
                                        event = new WebSocketMessageEvent(e.getSessionUUID(), decodedPayload, opCode, e.event);
                                        System.out.println(decodedPayload);
                                    } else {
                                        event = new WebSocketMessageEvent(e.getSessionUUID(),
                                                e.lastPartialMessage + decodedPayload, opCode, e.event);
                                        System.out.println(e.lastPartialMessage + decodedPayload);
                                    }
                                    e.lastPartialMessage = null;
                                } else {
                                    //noinspection StringConcatenationInLoop
                                    e.lastPartialMessage += decodedPayload;
                                    event = new FragmentedWebSocketEvent(e.getSessionUUID(), decodedPayload, opCode, e.event);
                                }
                                manager.fireEvent(event);

                                for (String toAll : event.getToSendToAll()) {
                                    byte[] toSend = generateFrame(toAll, OpCodes.TEXT);
                                    for (WebSocketClient client : connections) {
                                        try {
                                            sendFrame(client.getSocket(), toSend);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            toRemove.add(client);
                                        }
                                    }
                                }

                                for (Map.Entry<UUID, String> entry : event.getToSend().entrySet()) {
                                    sendFrame(entry.getKey(), entry.getValue(), OpCodes.TEXT);
                                    System.out.println(entry.getValue());
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
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
                        System.out.println("Removing client!");
                        e.socket.close();
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

    private static class WebSocketClient {
        private final Socket socket;
        private final UUID sessionUUID;
        private final PageRequestEvent event;
        long lastPing;
        String lastPartialMessage = null;

        WebSocketClient(Socket socket, PageRequestEvent event) {
            this.socket = socket;
            this.event = event;
            this.sessionUUID = event.getSessionUuid();
            this.lastPing = System.currentTimeMillis();
        }

        Socket getSocket() {
            return socket;
        }

        UUID getSessionUUID() {
            return sessionUUID;
        }
    }

    static final class OpCodes {
        public final static byte TEXT = 0x1;
        public final static byte CLOSE = 0x8;
        public final static byte PONG = 0xA;
        public final static byte PING = 0x9;

    }
}
