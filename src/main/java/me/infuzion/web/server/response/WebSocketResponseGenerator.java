package me.infuzion.web.server.response;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.FragmentedWebSocketEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.def.WebSocketEvent;
import me.infuzion.web.server.event.def.WebSocketMessageEvent;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public void sendFrame(UUID id, byte[] payload, byte opCode) throws IOException {
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

    protected byte[] generateFrame(String payload, byte opCode) {
        if (payload.length() < 100) {
            System.out.println("Sending " + payload + " with opCode: " + opCode);
        }
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        return generateFrame(payloadBytes, opCode);
    }

    protected byte[] generateFrame(byte[] payloadBytes, byte opCode) {
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
            BigInteger bi = BigInteger.valueOf(payloadBytes.length & ~Long.MIN_VALUE);
            ByteBuffer buffer = ByteBuffer.wrap(header);
            header[1] = 127;
            buffer.putLong(2, bi.longValue());
            dataStartIndex = 10;
        }
        byte[] toRet = new byte[dataStartIndex + payloadBytes.length];
        System.arraycopy(header, 0, toRet, 0, dataStartIndex);
        System.arraycopy(payloadBytes, 0, toRet, dataStartIndex, payloadBytes.length);
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
                            if (counter % 2000 == 0) {
                                sendFrame(e.getSocket(), generateFrame("ping", OpCodes.PING));
                            }

                            BufferedInputStream inputStream = e.inputStream;
                            if (inputStream.available() > 0) {
                                byte[] headers = new byte[2]; // Websocket headers
                                int size = inputStream.read(headers);

                                if (size != 2) {
                                    System.out.println(1);
                                    toRemove.add(e);
                                    continue;
                                }

                                boolean fin = ((headers[0] >> 7) & 1) != 0;
                                boolean masked = ((headers[1] >> 7) & 1) == 1;


                                long payloadSize = headers[1] & 127;
                                byte opCode = (byte) (headers[0] - ((headers[0] >> 4) << 4));


                                if (!masked) {
                                    // Client must mask connections to server
                                    System.out.println(2);
                                    toRemove.add(e);
                                    continue;
                                }

                                if (OpCodes.isReserved(opCode)) {
                                    //Reserved opcodes cannot be used
                                    System.out.println(3);
                                    toRemove.add(e);
                                    continue;
                                }

                                if (OpCodes.isControl(opCode) && !fin) {
                                    //Control frames cannot be continued
                                    System.out.println(4);
                                    toRemove.add(e);
                                    continue;
                                }

                                if (payloadSize == 126) {
                                    byte[] len = new byte[2];
                                    int read = inputStream.read(len);
                                    if (read != 2) {
                                        System.out.println(5);
                                        toRemove.add(e);
                                        continue;
                                    }
                                    ByteBuffer buffer = ByteBuffer.wrap(len);
                                    payloadSize = buffer.getShort(0) & 0xffff;
                                } else if (payloadSize == 127) {
                                    byte[] len = new byte[8];
                                    int read = inputStream.read(len);
                                    if (read != 8) {
                                        System.out.println(6);
                                        toRemove.add(e);
                                        continue;
                                    }
                                    ByteBuffer buffer = ByteBuffer.wrap(len);
                                    payloadSize = buffer.getLong(0);

                                    BigInteger bi = BigInteger.valueOf(payloadSize & ~Long.MIN_VALUE);
                                    if (payloadSize < 0) bi = bi.setBit(63);
                                    payloadSize = bi.longValue();
                                }

                                byte[] masks = new byte[4];
                                int masksRead = inputStream.read(masks);

                                if (masksRead != 4) {
                                    System.out.println(7);
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

                                String decodedPayload = new String(decoded, StandardCharsets.UTF_8);
                                e.lastPing = System.currentTimeMillis();

                                if (opCode == OpCodes.CLOSE) {
                                    toRemove.add(e);
                                    continue;
                                }
                                if (opCode == OpCodes.PING) {
                                    // Control frames can only have upto 125 bytes as a payload
                                    if (payloadSize > 125) {
                                        System.out.println(8);
                                        toRemove.add(e);
                                        continue;
                                    }
                                    sendFrame(e.getSocket(), generateFrame(decoded, OpCodes.PONG));
                                    continue;
                                }

                                if (opCode == OpCodes.PONG) {
                                    continue;
                                }

                                WebSocketEvent event;
                                if (fin) {
                                    if (e.lastPartialMessage == null) {
                                        event = new WebSocketMessageEvent(e.getSessionUUID(), decodedPayload, opCode, e.event);
                                    } else {
                                        e.lastPartialMessage.write(decoded);
                                        event = new WebSocketMessageEvent(e.getSessionUUID(),
                                                new String(e.lastPartialMessage.toByteArray(), StandardCharsets.UTF_8),
                                                opCode, e.event);
                                    }
                                    e.lastPartialMessage.reset();
                                    e.hasPartial = false;
                                } else {
                                    e.lastPartialMessage.write(decoded);
                                    event = new FragmentedWebSocketEvent(e.getSessionUUID(), decodedPayload, opCode, e.event);
                                    e.hasPartial = true;
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
                        try {
                            sendFrame(e.sessionUUID, "", OpCodes.CLOSE);
                        } catch (Exception ignored) {
                        }
                        e.socket.close();
                        connections.remove(e);
                    }
                    toRemove.clear();
                }
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class WebSocketClient {
        private final Socket socket;
        final PageRequestEvent event;
        private final UUID sessionUUID;
        private final BufferedInputStream inputStream;
        long lastPing;
        boolean hasPartial = false;
        ByteArrayOutputStream lastPartialMessage = new ByteArrayOutputStream();

        WebSocketClient(Socket socket, PageRequestEvent event) throws IOException {
            this.socket = socket;
            this.event = event;
            this.sessionUUID = event.getSessionUuid();
            this.lastPing = System.currentTimeMillis();
            inputStream = new BufferedInputStream(socket.getInputStream());
        }

        Socket getSocket() {
            return socket;
        }

        UUID getSessionUUID() {
            return sessionUUID;
        }
    }

    static final class OpCodes {
        public final static byte CONTINUATION = 0x0;
        public final static byte TEXT = 0x1;
        public final static byte BINARY = 0x2;
        public final static byte CLOSE = 0x8;
        public final static byte PING = 0x9;
        public final static byte PONG = 0xA;

        public static boolean isReserved(byte opcode) {
            return (3 <= opcode && opcode <= 7) || (0xB <= opcode && opcode <= 0xF);
        }

        public static boolean isControl(byte opcode) {
            return opcode == CLOSE || opcode == PONG || opcode == PING;
        }

    }
}
