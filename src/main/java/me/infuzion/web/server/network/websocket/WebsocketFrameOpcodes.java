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

import org.jetbrains.annotations.NotNull;

public enum WebsocketFrameOpcodes {
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

    WebsocketFrameOpcodes(byte value) {
        this.value = value;
    }

    static @NotNull WebsocketFrameOpcodes fromByteValue(byte val) {
        for (WebsocketFrameOpcodes opcode : WebsocketFrameOpcodes.values()) {
            if (opcode.value == val) {
                return opcode;
            }
        }

        throw new RuntimeException("Unknown Opcode " + val);
    }

    boolean isReserved() {
        return this == NON_CONTROL3 || this == NON_CONTROL4 || this == NON_CONTROL5 || this == NON_CONTROL6 ||
                this == NON_CONTROL7 || this == RESERVED_B || this == RESERVED_C || this == RESERVED_D ||
                this == RESERVED_E || this == RESERVED_F;
    }

    boolean isControl() {
        return this == PING || this == PONG || this == CLOSE;
    }
}
