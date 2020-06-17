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

import me.infuzion.web.server.Server;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import org.jetbrains.annotations.Nullable;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * Implementing classes handle the network communications between this server and the client. All clients are represented
 * by UUIDs. Implementing classes should avoid holding strong references to the UUID object unless needed.
 * <p>
 * ConnectionHandlers will run in a new thread, so implementing classes should synchronize on the clientUUID.
 */
public interface ConnectionHandler extends Runnable {
    void init(Server server, EventManager eventManager, Selector clientSelector);

    /**
     * Registers a client with this connection handler.
     *
     * @param clientUUID The UUID of the client to begin processing
     * @param event      The event that triggered this transfer, or null if no event triggered this registration.
     */
    void register(SocketChannel channel, UUID clientUUID, @Nullable Event event) throws Exception;

    /**
     * Begins handling connections. Implementing classes should iterate indefinitely while reading and writing
     * messages to clients.
     */
    void handleConnections() throws Exception;

    default void run() {
        try {
            handleConnections();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
