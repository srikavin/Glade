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
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.http.parser.*;
import me.infuzion.web.server.response.DefaultResponseGenerator;
import me.infuzion.web.server.response.ResponseGenerator;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class HttpConnectionHandler extends AbstractConnectionHandler {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final ResponseGenerator defaultResponseGenerator = new DefaultResponseGenerator();

    private final Map<UUID, Client> clientMap = Collections.synchronizedMap(new WeakHashMap<>());
    private final HttpParser parser = new HttpParser();
    private final List<BodyParser> bodyParsers = new ArrayList<>();

    public HttpConnectionHandler() {
        bodyParsers.add(new MultipartBodyParser());
        bodyParsers.add(new UrlEncodedBodyParser());
        bodyParsers.add(new JsonBodyParser());
    }

    @Override
    protected void handleNewClient(SocketChannel client, UUID uuid) {
        clientMap.put(uuid, new Client());
    }

    @Override
    protected void handleRead(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception {
        Client client = clientMap.get(uuid);

        if (client == null) {
            return;
        }

        if (client.buffer == null) {
            client.buffer = ByteBuffer.allocate(1024 * 1024 * 32);
        }
        int numRead = clientChannel.read(client.buffer);

        if (numRead == -1) {
            clientChannel.close();
            key.cancel();
            clients.remove(uuid);
            clientMap.remove(uuid);
            return;
        }

        HttpRequest request = parser.parse(client.buffer);

        if (request == null) {
            clientChannel.register(clientSelector, SelectionKey.OP_READ, uuid);
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
        event.setConnectionHandler(this.getClass());
        eventManager.fireEvent(event);

        client.headersToWrite = null;
        client.bodyToWrite = null;
        client.buffer = null;

        client.toTransfer = event.getConnectionHandler();

        ResponseGenerator generator = event.getResponseGenerator();
        ByteBuffer response = generator.generateResponse(event);

        if (response == null) {
            clientChannel.register(clientSelector, SelectionKey.OP_READ, uuid);
            return;
        }

        client.headersToWrite = response;

        if (generator.shouldCopyBody(event) && event.getResponse().getBody() != null) {
            client.bodyToWrite = event.getResponse().getBody();
        }

        clientChannel.register(clientSelector, SelectionKey.OP_WRITE, uuid);
    }

    @Override
    protected void handleWrite(SelectionKey key, UUID uuid, SocketChannel clientChannel) throws Exception {
        Client client = clientMap.get(uuid);
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
            // Only transfer after writing the expected reponse data
            if (client.toTransfer != null) {
                ConnectionHandler handler = server.getConnectionHandler(client.toTransfer);


                if (handler == null) {
                    logger.atSevere().log("No connection handler found for %s", client.toTransfer);
                    throw new RuntimeException("Connection Handler does not exist");
                }

                logger.atInfo().log("Transferring connection to %s", client.toTransfer);

                // Transfer this client to the registered connection handler
                handler.register(clientChannel, uuid);
                clients.remove(uuid);
                return;
            }

            clientChannel.register(clientSelector, SelectionKey.OP_READ, uuid);
        } else {
            clientChannel.register(clientSelector, SelectionKey.OP_WRITE, uuid);
        }
    }


    private static class Client {
        ByteBuffer buffer = null;
        ByteBuffer headersToWrite = null;
        ByteBuffer bodyToWrite = null;
        Class<? extends ConnectionHandler> toTransfer = null;
    }
}
