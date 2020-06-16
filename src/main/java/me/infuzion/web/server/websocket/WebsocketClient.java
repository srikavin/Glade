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

import me.infuzion.web.server.network.websocket.WebsocketConnectionHandler;
import me.infuzion.web.server.network.websocket.WebsocketFrameOpcodes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

public class WebsocketClient {
    private UUID uuid;
    private WebsocketConnectionHandler connectionHandler;

    public void send(String message, Charset charset) {

    }

    public void send(String message) {

    }

    public void sendBinary(ByteBuffer payload) {

    }

    public void sendFrame(WebsocketFrameOpcodes opcode, ByteBuffer payload) {

    }
}
