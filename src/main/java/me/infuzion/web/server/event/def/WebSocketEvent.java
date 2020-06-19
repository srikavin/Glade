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

import me.infuzion.web.server.event.AbstractEvent;
import me.infuzion.web.server.event.reflect.param.HasPath;
import me.infuzion.web.server.websocket.WebsocketClient;

public abstract class WebSocketEvent extends AbstractEvent implements HasPath {
    private final WebsocketClient client;

    protected WebSocketEvent(WebsocketClient client) {
        this.client = client;
    }

    public WebsocketClient getClient() {
        return client;
    }

    @Override
    public String getPath() {
        return client.getPath();
    }
}
