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

enum WebsocketFrameCloseCodes {
    NORMAL((short) 1000),
    GOING_AWAY((short) 1001),
    PROTOCOL_ERROR((short) 1002),
    INVALID_DATA((short) 1003),
    RESERVED((short) 1004),
    RESERVED_NO_STATUS((short) 1005),
    RESERVED_ABNORMAL((short) 1006),
    INCONSISTENT_DATA((short) 1007),
    POLICY_VIOLATION((short) 1008),
    MESSAGE_TOO_BIG((short) 1009),
    MISSING_EXTENSION((short) 1010),
    UNEXPECTED_CONDITION((short) 1011),
    RESERVED_TLS_FAILURE((short) 1015),
    OTHER((short) 4000),
    RESERVED_UNUSED((short) 1),
    RESERVED_FUTURE((short) 2000);

    short value;

    WebsocketFrameCloseCodes(short value) {
        this.value = value;
    }

    static @NotNull WebsocketFrameCloseCodes fromValue(short val) {
        for (WebsocketFrameCloseCodes opcode : WebsocketFrameCloseCodes.values()) {
            if (opcode.value == val) {
                return opcode;
            }
        }

        if (val <= 999) {
            return RESERVED_UNUSED;
        } else if (val <= 2999) {
            return RESERVED_FUTURE;
        }

        return OTHER;
    }

    boolean isReserved() {
        return this == RESERVED || this == RESERVED_ABNORMAL || this == RESERVED_NO_STATUS ||
                this == RESERVED_TLS_FAILURE || this == RESERVED_FUTURE || this == RESERVED_UNUSED;
    }
}
