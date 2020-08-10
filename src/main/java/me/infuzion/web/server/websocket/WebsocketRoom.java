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

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.WebSocketDisconnectEvent;
import me.infuzion.web.server.event.reflect.EventHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WebsocketRoom {
    private final Set<WebsocketClient> clients;

    public WebsocketRoom(EventManager manager) {
        this.clients = Collections.synchronizedSet(new HashSet<>());
        manager.registerListener(new EventListener() {
            @EventHandler
            public void removeHandler(WebSocketDisconnectEvent event) {
                clients.remove(event.getClient());
            }
        });
    }

    public void addClient(WebsocketClient client) {
        clients.add(client);
    }

    public void removeClient(UUID uuid) {
        clients.removeIf(client -> client.getId().equals(uuid));
    }

    public void removeClient(WebsocketClient client) {
        removeClient(client.getId());
    }

    public void sendToAll(String string) {
        for (WebsocketClient client : clients) {
            client.send(string);
        }
    }
}
