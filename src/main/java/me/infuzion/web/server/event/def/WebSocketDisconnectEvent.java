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

package me.infuzion.web.server.event.def;

import me.infuzion.web.server.network.websocket.WebsocketFrameCloseCodes;
import me.infuzion.web.server.websocket.WebsocketClient;
import org.jetbrains.annotations.Nullable;

public class WebSocketDisconnectEvent extends WebSocketEvent {
    private final WebsocketFrameCloseCodes closeCode;
    private final @Nullable String info;

    public WebSocketDisconnectEvent(WebsocketClient client, WebsocketFrameCloseCodes closeCode, @Nullable String info) {
        super(client);
        this.closeCode = closeCode;
        this.info = info;
    }

    public WebsocketFrameCloseCodes getCloseCode() {
        return closeCode;
    }

    public @Nullable String getInfo() {
        return info;
    }
}
