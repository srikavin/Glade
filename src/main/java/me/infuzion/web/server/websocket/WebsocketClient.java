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

package me.infuzion.web.server.websocket;

import me.infuzion.web.server.network.websocket.WebsocketFrameOpcodes;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Stored references to implementing classes should generally be avoided as implementations may store
 * network buffers. References to the UUID of the client should be stored.
 */
public interface WebsocketClient {

    UUID getId();

    /**
     * Sends a UTF-8 encoded string to this websocket client
     *
     * @param message A UTF-8 encodeable string
     */
    void send(String message);

    /**
     * Sends a binary message to this websocket client
     *
     * @param payload A byte buffer containing a binary payload
     */
    void sendBinary(ByteBuffer payload);


    /**
     * Sends a frame with the specified opcode and payload to the websocket client
     *
     * @param opcode  The opcode of the frame
     * @param payload A byte buffer containing the payload
     */
    void sendFrame(WebsocketFrameOpcodes opcode, ByteBuffer payload);

    /**
     * Disconnects this client
     */
    void remove();

    /**
     * Gets the path the websocket is connected to
     */
    String getPath();

    boolean isConnected();
}
