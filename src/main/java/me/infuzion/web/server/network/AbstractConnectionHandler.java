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

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.Server;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.EventManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public abstract class AbstractConnectionHandler implements ConnectionHandler {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /**
     * A set containing all of the clients managed by this connection handler.
     */
    protected final Set<UUID> clients = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * A set containing all of the clients that are to be removed
     */
    protected final Set<UUID> removedClients = Collections.newSetFromMap(new WeakHashMap<>());

    protected Server server;
    protected EventManager eventManager;
    protected Selector clientSelector;


    @Override
    public void register(SocketChannel client, UUID clientUUID, Event event) throws ClosedChannelException {
        clients.add(clientUUID);

        handleNewClient(client, clientUUID, event);

        client.register(clientSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, clientUUID);
        clientSelector.wakeup();
    }

    @Override
    public void init(Server server, EventManager eventManager, Selector clientSelector) {
        this.server = server;
        this.eventManager = eventManager;
        this.clientSelector = clientSelector;
    }

    public void handleConnections() throws IOException {
        while (true) {
            int readyCount = clientSelector.select();

            if (readyCount == 0) {
                continue;
            }

            Set<SelectionKey> ready = clientSelector.selectedKeys();
            Iterator<SelectionKey> iterator = ready.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                UUID uuid = (UUID) key.attachment();

                if (!clients.contains(uuid)) {
                    if (removedClients.contains(uuid)) {
                        SocketChannel client = (SocketChannel) key.channel();
                        client.close();
                        key.cancel();
                        removedClients.remove(uuid);
                    }
                    continue;
                }

                SocketChannel client = (SocketChannel) key.channel();

                try {
                    if (key.isValid() && key.isReadable()) {
                        handleRead(key, uuid, client);
                    }
                    if (key.isValid() && key.isWritable()) {
                        handleWrite(key, uuid, client);
                    }
                } catch (Exception e) {
                    logger.atWarning().withCause(e).log("Exception occurred");
                    removeClient(uuid);
                }
            }
        }
    }

    /**
     * Called when a registered channel is available to read.
     */
    protected abstract void handleRead(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception;

    /**
     * Called when a registered channel is available to write.
     */
    protected abstract void handleWrite(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception;

    protected abstract void handleNewClient(SocketChannel client, UUID uuid, @Nullable Event event);

    protected void removeClient(UUID uuid) {
        clients.remove(uuid);
        removedClients.add(uuid);

        try {
            handleRemoveClient(uuid);
        } catch (Exception e) {
            logger.atWarning().withCause(e).log("Exception occurred while removing client");
        }
    }

    protected abstract void handleRemoveClient(UUID uuid) throws Exception;
}
