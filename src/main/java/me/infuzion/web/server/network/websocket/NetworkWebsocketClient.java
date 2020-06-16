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

import me.infuzion.web.server.websocket.WebsocketClient;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

class NetworkWebsocketClient implements WebsocketClient {

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

    boolean shouldClose = false;

    NetworkWebsocketClient(UUID uuid) {
    }

    /**
     * Close the client connection after writing out all current messages
     */
    void reset() {
        this.previousPayloads.clear();
        this.readBuffer = null;
        this.primaryFrame = null;
        this.curFrame = null;
        this.state = WebsocketClientParserState.HEADER;
        this.headerTotalRead = 0;
    }

    @Override
    public UUID getId() {
        return null;
    }

    @Override
    public void send(String message) {
        this.sendFrame(WebsocketFrameOpcodes.TEXT, StandardCharsets.UTF_8.encode(message));
    }

    @Override
    public void sendBinary(ByteBuffer payload) {
        this.sendFrame(WebsocketFrameOpcodes.BINARY, payload);
    }

    @Override
    public void sendFrame(WebsocketFrameOpcodes opcode, ByteBuffer payload) {
        WebsocketFrameHeader header = new WebsocketFrameHeader();
        header.fin = true;
        header.payloadLength = payload.limit();
        header.opcode = opcode;

        writeFrame(header, payload);
    }

    void writeFrame(WebsocketFrameHeader header, ByteBuffer payload) {
        writeBuffer.add(header.toByteBuffer(payload));
    }

    @Override
    public void remove() {
        this.shouldClose = true;
    }

    @Override
    public boolean isConnected() {
        return !this.shouldClose;
    }

    enum WebsocketClientParserState {
        HEADER,
        PAYLOAD
    }
}
