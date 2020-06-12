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

package me.infuzion.web.server;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.EventPriority;
import me.infuzion.web.server.http.parser.*;
import me.infuzion.web.server.response.DefaultResponseGenerator;
import me.infuzion.web.server.response.ResponseGenerator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    public static final String version = "1.5.0";
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    static {
        String path = Server.class
                .getClassLoader()
                .getResource("logging.properties")
                .getFile();
        System.setProperty("java.util.logging.config.file", path);
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    private final HttpParser parser = new HttpParser();
    private final ServerSocketChannel serverSocketChannel;
    private final Selector clientSelector;
    private final Map<UUID, Client> clientMap = new HashMap<>();
    private final EventManager eventManager = new EventManager();
    private final ResponseGenerator defaultResponseGenerator = new DefaultResponseGenerator();
    private final List<BodyParser> bodyParsers = new ArrayList<>();

    public Server(InetSocketAddress address) throws IOException {
        logger.atInfo().log("Starting server at %s", address);
        clientSelector = Selector.open();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(address);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(clientSelector, SelectionKey.OP_ACCEPT);

        eventManager.registerListener(new EventListener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void handle(PageRequestEvent e) {
                logger.atInfo().log("Served %s in %.4f ms", e.getPath(), (System.nanoTime() - e.getCreationTime()) / 1.e6);
            }
        });

        bodyParsers.add(new MultipartBodyParser());
    }

    private void handleRead(SelectionKey key, SocketChannel clientChannel, Client client) throws Exception {
        if (client.buffer == null) {
            client.buffer = ByteBuffer.allocate(1024 * 1024 * 32);
        }
        int numRead = clientChannel.read(client.buffer);

        if (numRead == -1) {
            clientChannel.close();
            key.cancel();
            return;
        }

        HttpRequest request = parser.parse(client.buffer);

        if (request == null) {
            clientChannel.register(clientSelector, SelectionKey.OP_READ, client.uuid);
            return;
        }

        BodyData bodyData = new BodyData(Collections.emptyMap());
        if (request.getRawBody() != null) {
            ByteBuffer raw = request.getRawBody();
            for (BodyParser parser : bodyParsers) {
                if (parser.matches(request, raw)) {
                    bodyData = parser.parse(request, raw);
                    break;
                }
            }
        }

        PageRequestEvent event = new PageRequestEvent(request, bodyData);
        event.setResponseGenerator(defaultResponseGenerator);
        eventManager.fireEvent(event);


        ResponseGenerator generator = event.getResponseGenerator();
        ByteBuffer response = generator.generateResponse(event);

        client.headersToWrite = null;
        client.bodyToWrite = null;
        client.buffer = null;

        if (response == null) {
            clientChannel.register(clientSelector, SelectionKey.OP_READ, client.uuid);
            return;
        }

        client.headersToWrite = response;

        if (generator.shouldCopyBody(event) && event.getResponse().getBody() != null) {
            client.bodyToWrite = event.getResponse().getBody();
        }

        clientChannel.register(clientSelector, SelectionKey.OP_WRITE, client.uuid);
    }

    private void handleWrite(SelectionKey key, SocketChannel clientChannel, Client client) throws IOException {
        if (client.headersToWrite != null) {
            clientChannel.write(client.headersToWrite);

            if (client.headersToWrite.limit() == client.headersToWrite.position()) {
                client.headersToWrite = null;
            }
        }

        if (client.headersToWrite == null && client.bodyToWrite != null) {
            clientChannel.write(client.bodyToWrite);

            if (client.bodyToWrite.limit() == client.bodyToWrite.position()) {
                client.bodyToWrite = null;
            }
        }

        if (client.headersToWrite == null && client.bodyToWrite == null) {
            clientChannel.register(clientSelector, SelectionKey.OP_READ, client.uuid);
        } else {
            clientChannel.register(clientSelector, SelectionKey.OP_WRITE, client.uuid);
        }
    }

    public void start() throws IOException {
        while (true) {
            Set<UUID> clientsUUIDS = clientSelector.keys().stream().map(e -> (UUID) e.attachment()).collect(Collectors.toSet());
            clientMap.entrySet().removeIf(next -> !clientsUUIDS.contains(next.getKey()));

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
                    UUID uuid = (UUID) key.attachment();
                    clientMap.remove(uuid);
                    key.cancel();
                    continue;
                }

                if (key.isAcceptable()) {
                    UUID uuid = UUID.randomUUID();
                    clientMap.put(uuid, new Client(uuid));

                    try {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();

                        client.configureBlocking(false);

                        key.attach(uuid);

                        client.register(clientSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, uuid);
                    } catch (Exception e) {
                        logger.atWarning().withCause(e).log("Exception occurred %s", e);
                        clientMap.remove(uuid);
                        key.cancel();
                    }
                    continue;
                }


                SocketChannel client = (SocketChannel) key.channel();
                UUID uuid = (UUID) key.attachment();
                Client c = clientMap.get(uuid);

                try {
                    if (key.isValid() && key.isReadable()) {
                        handleRead(key, client, c);
                    }
                    if (key.isValid() && key.isWritable()) {
                        handleWrite(key, client, c);
                    }
                } catch (Exception e) {
                    logger.atWarning().withCause(e).log("Exception occurred %s", e);
                    clientMap.remove(uuid);
                    client.close();
                    key.cancel();
                }
            }
        }
    }

    private static class Client {
        ByteBuffer buffer = null;

        UUID uuid;

        ByteBuffer headersToWrite = null;
        ByteBuffer bodyToWrite = null;

        public Client(UUID uuid) {
            this.uuid = uuid;
        }

    }
}
