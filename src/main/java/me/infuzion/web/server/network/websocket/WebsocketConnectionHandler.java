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

package me.infuzion.web.server.network.websocket;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.network.AbstractConnectionHandler;
import me.infuzion.web.server.util.ByteBufferUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles websocket connections. Connection upgrades should be finalized before being registered with this connection
 * handler.
 */
public class WebsocketConnectionHandler extends AbstractConnectionHandler {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Map<UUID, WebsocketNetworkState> clientMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();

    @Override
    protected void handleNewClient(SocketChannel client, UUID uuid) {
        clientMap.put(uuid, new WebsocketNetworkState());
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

        WebsocketNetworkState client = clientMap.get(uuid);


        if (client == null) {
            return;
        }

        if (client.shouldClose) {
            return;
        }

        if (client.state == WebsocketNetworkState.WebsocketClientParserState.HEADER) {
            readHeader(client, clientChannel);
        }

        if (client.state == WebsocketNetworkState.WebsocketClientParserState.PAYLOAD) {
            readPayload(client, clientChannel);
        }
    }

    private void writeCloseFrame(WebsocketFrameCloseCodes closeCode, WebsocketNetworkState client) {
        writeCloseFrame(closeCode, null, client);
    }

    private void writeCloseFrame(WebsocketFrameCloseCodes closeCode, @Nullable String additionalData, WebsocketNetworkState client) {
        ByteBuffer buffer;
        if (additionalData != null) {
            ByteBuffer encoded = StandardCharsets.UTF_8.encode(additionalData);
            buffer = ByteBuffer.allocate(2 + encoded.limit());
            buffer.putShort(closeCode.value);
            buffer.put(encoded);
        } else {
            buffer = ByteBuffer.allocate(2);
            buffer.putShort(closeCode.value);
        }
        buffer.rewind();
        writeFrame(WebsocketFrameOpcodes.CLOSE, buffer, client);

        client.shouldClose = true;
    }

    @Override
    protected void handleWrite(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception {
        WebsocketNetworkState client = clientMap.get(uuid);

        ByteBuffer buffer = client.writeBuffer.peek();


        if (buffer != null) {
            clientChannel.write(buffer);

            if (!buffer.hasRemaining()) {
                client.writeBuffer.poll();
            }
        }

        if (client.writeBuffer.isEmpty() && client.shouldClose) {
            clients.remove(uuid);
            key.channel().close();
            key.cancel();
        }
    }

    private @Nullable WebsocketFrameCloseCodes validateClientHeader(WebsocketFrameHeader header, @Nullable WebsocketFrameHeader primaryHeader) {
        if (!header.mask) {
            logger.atWarning().log("Connecting websocket had invalid frame header (no mask)");
            return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
        }

        if (header.rsv1 || header.rsv2 || header.rsv3) {
            logger.atWarning().log("Connecting websocket had invalid frame header (reserved bit set)");
            return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
        }

        if (header.opcode.isReserved()) {
            logger.atWarning().log("Connecting websocket had invalid frame header (reserved opcode used)");
            return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
        }

        if (header.opcode.isControl() && !header.fin) {
            logger.atWarning().log("Connecting websocket had invalid frame header (control frame without fin set)");
            return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
        }

        if (header.opcode.isControl() && header.payloadLength > 125) {
            logger.atWarning().log("Connecting websocket had invalid frame header (control frame with payload > 125)");
            return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
        }

        if (header.payloadLength > 1024 * 1024 * 32) {
            logger.atWarning().log("Connecting websocket had invalid frame header (exceeded maximum payload size)");
            return WebsocketFrameCloseCodes.MESSAGE_TOO_BIG;
        }

        if (!header.opcode.isControl()) {
            if (header.opcode == WebsocketFrameOpcodes.CONTINUATION && primaryHeader == null) {
                logger.atWarning().log("Connecting websocket had invalid frame header (continuation with no preceding unfinished frame)");
                return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
            }

            if (header.opcode != WebsocketFrameOpcodes.CONTINUATION && primaryHeader != null && !primaryHeader.fin) {
                logger.atWarning().log("Connecting websocket had invalid frame header (non continuation with preceding unfinished frame)");
                return WebsocketFrameCloseCodes.PROTOCOL_ERROR;
            }
        }

        return null;
    }

    /**
     * Handles reading the metadata of a websocket frame.
     */
    private void readHeader(WebsocketNetworkState client, SocketChannel channel) throws IOException {
        if (client.readBuffer == null) {
            client.readBuffer = ByteBuffer.allocate(16);
            client.curFrame = new WebsocketFrameHeader();
        }

        ByteBuffer buffer = client.readBuffer;
        buffer.position(client.headerTotalRead);

        int nRead;

        if (client.nextFrameData != null) {
            nRead = client.nextFrameData.remaining();
            client.readBuffer.put(client.nextFrameData);
            client.nextFrameData = null;
        } else {
            nRead = channel.read(buffer);
        }

        if (nRead == -1) {
            writeCloseFrame(WebsocketFrameCloseCodes.NORMAL, client);
            return;
        }

        client.headerTotalRead += nRead;

        if (client.curFrame == null) {
            client.curFrame = new WebsocketFrameHeader();
        }

        WebsocketFrameHeader header = client.curFrame;

        buffer.rewind();
        if (!header.parseFrom(buffer, client.headerTotalRead)) {
            // wait for more data
            return;
        }

        WebsocketFrameCloseCodes errorCode = validateClientHeader(header, client.primaryFrame);
        if (errorCode != null) {
            writeCloseFrame(errorCode, client);
            return;
        }

        ByteBuffer payload = ByteBuffer.allocate(header.payloadLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.toIntExact(header.payloadLength));

        // move remaining data into payload buffer
        if (payload.limit() < buffer.remaining()) {
            // handle next frame data that was read
            buffer.mark();
            buffer.position(buffer.position() + payload.limit());
            client.nextFrameData = buffer.slice();
            buffer.limit(buffer.position());
            buffer.reset();
        }

        payload.put(buffer);

        client.headerTotalRead = 0;

        if (header.opcode != WebsocketFrameOpcodes.CONTINUATION && !header.opcode.isControl()) {
            client.primaryFrame = header;
        }

        client.readBuffer = payload;
        client.state = WebsocketNetworkState.WebsocketClientParserState.PAYLOAD;
    }

    private void handleControlFrame(WebsocketFrameHeader header, ByteBuffer payload, WebsocketNetworkState client) {
        WebsocketFrameOpcodes opcode = header.opcode;
        if (opcode == WebsocketFrameOpcodes.PING) {
            writeFrame(WebsocketFrameOpcodes.PONG, payload, client);
            return;
        }

        if (opcode == WebsocketFrameOpcodes.PONG) {
            // do nothing
            return;
        }

        if (opcode == WebsocketFrameOpcodes.CLOSE) {
            if (payload.limit() == 0) {
                writeCloseFrame(WebsocketFrameCloseCodes.NORMAL, client);
                return;

            }
            if (payload.limit() == 1) {
                logger.atWarning().log("Websocket client sent invalid 1-byte close code");
                writeCloseFrame(WebsocketFrameCloseCodes.PROTOCOL_ERROR, client);
                return;
            }

            WebsocketFrameCloseCodes closeCode = WebsocketFrameCloseCodes.fromValue(payload.getShort(0));
            if (closeCode.isReserved()) {
                logger.atWarning().log("Websocket client sent invalid close code %d (%s)", closeCode.value, closeCode);
                writeCloseFrame(WebsocketFrameCloseCodes.PROTOCOL_ERROR, client);
                return;
            }

            writeCloseFrame(WebsocketFrameCloseCodes.NORMAL, client);
        }
    }

    private void handlePayload(WebsocketNetworkState client) {
        List<ByteBuffer> buffers = client.previousPayloads;

        if (client.curFrame.opcode.isControl()) {
            return;
        }

        if (buffers.size() == 0) {
            throw new RuntimeException("No buffers stored as payload!");
        }

        ByteBuffer consolidated;

        if (!client.curFrame.fin) {
            return;
        }

        if (client.primaryFrame != null) {
            int totalSize = 0;
            for (ByteBuffer e : buffers) {
                totalSize += e.limit();
            }

            consolidated = ByteBuffer.allocate(totalSize);

            for (ByteBuffer e : buffers) {
                e.rewind();
                consolidated.put(e);
            }
        } else {
            // Single frame
            consolidated = buffers.get(0);
        }
        consolidated.rewind();

        if (client.primaryFrame.opcode == WebsocketFrameOpcodes.TEXT) {
            try {
                utf8Decoder.decode(consolidated);
                writeFrame(client.primaryFrame.opcode, consolidated, client);
            } catch (CharacterCodingException e) {
                logger.atWarning().log("Websocket client sent invalid UTF-8 data");
                writeCloseFrame(WebsocketFrameCloseCodes.INCONSISTENT_DATA, client);
            }
        } else if (client.primaryFrame.opcode == WebsocketFrameOpcodes.BINARY) {
            writeFrame(client.primaryFrame.opcode, consolidated, client);
        }

        //TODO: Send websocket event

        client.reset();
    }

    private void readPayload(WebsocketNetworkState client, SocketChannel channel) throws IOException {
        ByteBuffer buffer = client.readBuffer;
        channel.read(buffer);

        if (!buffer.hasRemaining()) {
            byte[] array = ByteBufferUtils.getAsArray(buffer);

            // decode mask
            for (int i = 0; i < array.length; i++) {
                array[i] ^= client.curFrame.maskingKey[i % 4];
            }

            ByteBuffer wrapped = ByteBuffer.wrap(array);

            if (client.curFrame.opcode.isControl()) {
                handleControlFrame(client.curFrame, wrapped, client);
            } else {
                client.previousPayloads.add(wrapped);
            }


            handlePayload(client);

            // do not change continuation sequence
            client.readBuffer = null;
            client.curFrame = null;
            client.state = WebsocketNetworkState.WebsocketClientParserState.HEADER;
        }
    }


    public void writeFrame(WebsocketFrameHeader header, ByteBuffer payload, WebsocketNetworkState client) {
        client.writeBuffer.add(header.toByteBuffer(payload));
    }

    public void writeFrame(WebsocketFrameOpcodes opcode, String payload, WebsocketNetworkState client) {
        writeFrame(opcode, StandardCharsets.UTF_8.encode(payload), client);
    }

    public void writeFrame(WebsocketFrameOpcodes opcode, ByteBuffer payload, WebsocketNetworkState client) {
        WebsocketFrameHeader header = new WebsocketFrameHeader();
        header.fin = true;
        header.payloadLength = payload.limit();
        header.opcode = opcode;

        writeFrame(header, payload, client);
    }

    static class WebsocketNetworkState {

        /**
         * A list containing all payloads in the current continuation sequence
         */
        List<ByteBuffer> previousPayloads = new ArrayList<>();

        ByteBuffer readBuffer = null;

        /**
         * The first frame of a series of continued messages. If this frame is not null, then the next non-control frame
         * should be treated as a continuation to this frame.
         */
        WebsocketFrameHeader primaryFrame = null;

        ByteBuffer nextFrameData = null;

        /**
         * The current frame, may not be a continuation of the primary frame (only if this frame is a control frame)
         */
        WebsocketFrameHeader curFrame = null;

        /**
         * The current state of parsing the websocket frame
         */
        WebsocketClientParserState state = WebsocketClientParserState.HEADER;

        /**
         * The total number of bytes read in the header (may include bytes read partially into the payload)
         */
        int headerTotalRead = 0;

        /**
         * A queue containing bytebuffers to be written back to the client. The data is assumed to begin at position 0
         * and end at the buffer's limit
         */
        Queue<ByteBuffer> writeBuffer = new LinkedList<>();

        /**
         * Close the client connection after writing out all current messages
         */
        boolean shouldClose = false;

        void reset() {
            this.previousPayloads.clear();
            this.readBuffer = null;
            this.primaryFrame = null;
            this.curFrame = null;
            this.state = WebsocketClientParserState.HEADER;
            this.headerTotalRead = 0;
        }

        enum WebsocketClientParserState {
            HEADER,
            PAYLOAD
        }
    }
}
