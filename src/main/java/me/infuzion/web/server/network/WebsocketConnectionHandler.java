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

package me.infuzion.web.server.network;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.util.ByteBufferUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Handles websocket connections. Connection upgrades should be finalized before being registered with this connection
 * handler.
 */
public class WebsocketConnectionHandler extends AbstractConnectionHandler {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Map<UUID, WebsocketClient> clientMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    protected void handleNewClient(SocketChannel client, UUID uuid) {
        clientMap.put(uuid, new WebsocketClient());
    }

    private void removeClient(UUID uuid, SocketChannel channel, byte reason) {

    }

    private boolean validateHeader(WebsocketFrameHeader header) {
        if (!header.mask) {
            logger.atWarning().log("Connecting websocket had invalid frame header (no mask)");
            return false;
        }

        if (header.rsv1 || header.rsv2 || header.rsv3) {
            logger.atWarning().log("Connecting websocket had invalid frame header (reserved bit set)");
            return false;
        }

        if (header.opcode.isReserved()) {
            logger.atWarning().log("Connecting websocket had invalid frame header (reserved opcode used)");
            return false;
        }

        return true;
    }

    /**
     * Handles reading the metadata of a websocket frame.
     */
    private void readHeader(UUID uuid, WebsocketClient client, SocketChannel channel) throws IOException {
        if (client.curBuffer == null) {
            client.curBuffer = ByteBuffer.allocate(16);
            client.curFrame = new WebsocketFrameHeader();
        }
        /*
              0               1               2               3
              0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             +-+-+-+-+-------+-+-------------+-------------------------------+
             |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
             |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
             |N|V|V|V|       |S|             |   (if payload len==126/127)   |
             | |1|2|3|       |K|             |                               |
             +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
             |     Extended payload length continued, if payload len == 127  |
             + - - - - - - - - - - - - - - - +-------------------------------+
             |                               |Masking-key, if MASK set to 1  |
             +-------------------------------+-------------------------------+

          https://tools.ietf.org/html/rfc6455#section-5.2
        */

        ByteBuffer buffer = client.curBuffer;
        int nRead = channel.read(buffer);

        client.headerTotalRead += nRead;


        if (nRead == -1) {
            // TODO: remove client
            return;
        }

        WebsocketFrameHeader header = client.curFrame;
        if (buffer.position() >= 1) {
            byte b0 = buffer.get(0);

            header.fin = (b0 >> 7) != 0;
            header.rsv1 = ((b0 >> 6) & 1) != 0;
            header.rsv2 = ((b0 >> 5) & 1) != 0;
            header.rsv3 = ((b0 >> 4) & 1) != 0;
            header.opcode = OpCodes.fromByteValue((byte) (b0 & 0b1111));
        } else {
            return;
        }

        if (buffer.position() >= 2) {
            byte b1 = buffer.get(1);

            header.mask = (b1 >> 7) != 0;
            header.payloadLength = ((b1 & (byte) 0b0111_1111)) & 0xFF;
        } else {
            return;
        }

        long payloadLength = header.payloadLength;

        int payloadLengthEndPosition = 2;

        if (payloadLength == 126) {
            // Interpret the next 2 bytes as an unsigned 16-bit integer
            if (buffer.position() < 4) {
                // Wait for more data to arrive
                return;
            }

            header.payloadLength = buffer.getShort(2) & 0xFF_FF;
            payloadLengthEndPosition = 4;
        } else if (payloadLength == 127) {
            // Interpret the next 8 bytes as an unsigned 64-bit integer
            if (buffer.position() < 10) {
                // Wait for more data to arrive
                return;
            }

//            payloadLengthEndPosition = 10;

            //TODO: Too big, remove client
            return;
        }

        int headerEndPosition = payloadLengthEndPosition;

        if (buffer.position() >= payloadLengthEndPosition + 4 && header.mask) {
            header.maskingKey = new byte[]{
                    buffer.get(payloadLengthEndPosition),
                    buffer.get(payloadLengthEndPosition + 1),
                    buffer.get(payloadLengthEndPosition + 2),
                    buffer.get(payloadLengthEndPosition + 3)
            };
            headerEndPosition += 4;
        } else {
            return;
        }

        validateHeader(header);

        ByteBuffer payload = ByteBuffer.allocate(header.payloadLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.toIntExact(header.payloadLength));
        // move remaining data into payload buffer

        buffer.position(headerEndPosition);
        buffer.limit(client.headerTotalRead);
        payload.put(buffer);

        if (header.opcode != OpCodes.CONTINUATION) {
            client.primaryFrame = header;
            client.continued = false;
        } else {
            client.continued = true;
        }

        client.curBuffer = payload;
        client.state = WebsocketClientState.PAYLOAD;
    }

    private void readPayload(UUID uuid, WebsocketClient client, SocketChannel channel) throws IOException {
        ByteBuffer buffer = client.curBuffer;
        channel.read(buffer);

        if (!buffer.hasRemaining()) {
            if (client.curFrame.opcode == OpCodes.CONTINUATION) {
                client.previousPayloads.add(buffer);
            }

            byte[] array = ByteBufferUtils.getAsArray(buffer);

            // decode mask

            for (int i = 0; i < array.length; i++) {
                array[i] ^= client.curFrame.maskingKey[i % 4];
            }

            //TODO: Construct event

            WebsocketFrameHeader header = new WebsocketFrameHeader();
            header.fin = true;
            header.opcode = OpCodes.TEXT;

            writeFrame(header, ByteBuffer.wrap(array), uuid);

            client.curFrame = null;
            client.previousPayloads.clear();
            client.continued = false;
            client.primaryFrame = null;
            client.curBuffer = null;
            client.state = WebsocketClientState.HEADER;
            client.headerTotalRead = 0;
        }
    }

    @Override
    protected void handleRead(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception {
        /*
              0               1               2               3
              0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             +-------------------------------+-------------------------------+
             | Masking-key (continued)       |          Payload Data         |
             +-------------------------------- - - - - - - - - - - - - - - - +
             :                     Payload Data continued ...                :
             + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
             |                     Payload Data continued ...                |
             +---------------------------------------------------------------+

          https://tools.ietf.org/html/rfc6455#section-5.2
        */

        WebsocketClient client = clientMap.get(uuid);

        if (client == null) {
            return;
        }

        if (client.state == WebsocketClientState.HEADER) {
            readHeader(uuid, client, clientChannel);
        }

        if (client.state == WebsocketClientState.PAYLOAD) {
            readPayload(uuid, client, clientChannel);
        }

        clientChannel.register(clientSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, uuid);
    }

    private ByteBuffer getFrameAsByteBuffer(WebsocketFrameHeader header, ByteBuffer payload) {
        byte b0 = 0;

        b0 += header.fin ? 1 << 7 : 0;
        b0 += header.rsv1 ? 1 << 6 : 0;
        b0 += header.rsv2 ? 1 << 5 : 0;
        b0 += header.rsv3 ? 1 << 4 : 0;
        b0 += header.opcode.value;

        byte b1 = 0;

        // ignore mask bit
        // b1 += header.mask ? 0 : 1 << 7;

        ByteBuffer buffer;

        int payloadSize = payload.limit();

        if (payloadSize <= 125) {
            buffer = ByteBuffer.allocate(2 + payloadSize);
            b1 += payloadSize;
        } else if (payloadSize <= 65536) {
            buffer = ByteBuffer.allocate(2 + 4 + payloadSize);
            b1 += 126;
        } else {
            buffer = ByteBuffer.allocate(2 + 8 + payloadSize);
            b1 += 127;
        }

        buffer.put(b0);
        buffer.put(b1);

        if (payloadSize > 125 && payloadSize <= 65535) {
            buffer.putInt(payloadSize);
        } else if (payloadSize > 125) {
            buffer.putLong(payloadSize);
        }

        payload.rewind();
        buffer.put(payload);

        return buffer.rewind();
    }

    private void writeFrame(WebsocketFrameHeader header, ByteBuffer payload, UUID uuid) {
        WebsocketClient client = clientMap.get(uuid);

        client.writeBuffer.add(getFrameAsByteBuffer(header, payload));
    }

    @Override
    protected void handleWrite(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception {
        WebsocketClient client = clientMap.get(uuid);

        ByteBuffer buffer = client.writeBuffer.peek();

        if (buffer != null) {
            clientChannel.write(buffer);

            if (!buffer.hasRemaining()) {
                client.writeBuffer.poll();
            }
        }
    }

    private enum WebsocketClientState {
        HEADER,
        PAYLOAD
    }

    private enum OpCodes {
        CONTINUATION((byte) 0),
        TEXT((byte) 1),
        BINARY((byte) 2),
        NON_CONTROL3((byte) 3),
        NON_CONTROL4((byte) 4),
        NON_CONTROL5((byte) 5),
        NON_CONTROL6((byte) 6),
        NON_CONTROL7((byte) 7),
        CLOSE((byte) 8),
        PING((byte) 9),
        PONG((byte) 0xA),
        RESERVED_B((byte) 0xB),
        RESERVED_C((byte) 0xC),
        RESERVED_D((byte) 0xD),
        RESERVED_E((byte) 0xE),
        RESERVED_F((byte) 0xF);

        byte value;

        OpCodes(byte value) {
            this.value = value;
        }

        static @NotNull OpCodes fromByteValue(byte val) {
            for (OpCodes opcode : OpCodes.values()) {
                if (opcode.value == val) {
                    return opcode;
                }
            }

            throw new RuntimeException("Unknown OpCode " + val);
        }

        boolean isReserved() {
            return this.value >= 0xB;
        }

        boolean isControl() {
            return this == PING || this == PONG || this == CLOSE;
        }
    }

    private static class WebsocketClient {
        List<ByteBuffer> previousPayloads = new ArrayList<>();
        ByteBuffer curBuffer = null;
        boolean continued = false;
        WebsocketFrameHeader curFrame = null;
        WebsocketFrameHeader primaryFrame = null;
        WebsocketClientState state = WebsocketClientState.HEADER;
        int headerTotalRead = 0;
        Queue<ByteBuffer> writeBuffer = new LinkedList<>();
    }

    private static class WebsocketFrameHeader {
        /**
         * 1 bit
         * Indicates if this frame is the final fragment of a message
         */
        boolean fin;
        /**
         * 1 bit
         * Reserved for extension use
         */
        boolean rsv1;
        /**
         * 1 bit
         * Reserved for extension use
         */
        boolean rsv2;
        /**
         * 1 bit
         * Reserved for extension use
         */
        boolean rsv3;
        /**
         * 4 bits
         */
        OpCodes opcode;
        /**
         * 1 bit
         * Whether the payload is masked
         */
        boolean mask;
        /**
         * 1 bit
         * 7, 7 + 16, or 7 + 64 bits
         * The length of the payload data
         */
        long payloadLength;
        /**
         * 0 or 4 bytes
         * Only present if {@link #mask} is true
         */
        byte[] maskingKey;

        @Override
        public String toString() {
            return "WebsocketFrameHeader{" +
                    "fin=" + fin +
                    ", rsv1=" + rsv1 +
                    ", rsv2=" + rsv2 +
                    ", rsv3=" + rsv3 +
                    ", opcode=" + opcode +
                    ", mask=" + mask +
                    ", payloadLength=" + payloadLength +
                    ", maskingKey=" + Arrays.toString(maskingKey) +
                    '}';
        }
    }
}
