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
import me.infuzion.web.server.event.reflect.Route;
import me.infuzion.web.server.event.reflect.param.DefaultTypeConverter;
import me.infuzion.web.server.event.reflect.param.TypeConverter;
import me.infuzion.web.server.event.reflect.param.mapper.impl.*;
import me.infuzion.web.server.listener.WebSocketListener;
import me.infuzion.web.server.network.ConnectionHandler;
import me.infuzion.web.server.network.HttpConnectionHandler;
import me.infuzion.web.server.network.websocket.WebsocketConnectionHandler;
import me.infuzion.web.server.router.Router;
import me.infuzion.web.server.router.def.DefaultRouter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    public static final String version = "1.6.4";
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final EventManager eventManager;
    private final ServerSocketChannel serverSocketChannel;
    private final Map<Class<? extends ConnectionHandler>, ConnectionHandler> connectionHandlers = new ConcurrentHashMap<>();
    private Class<? extends ConnectionHandler> defaultHandler;

    public Server(InetSocketAddress address) throws IOException {
        this(address, new DefaultTypeConverter());
    }

    public Server(InetSocketAddress address, TypeConverter typeConverter) throws IOException {
        logger.atInfo().log("Starting server at %s", address);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(address);
        serverSocketChannel.configureBlocking(false);

        Router router = new DefaultRouter();

        eventManager = new EventManager();
        eventManager.registerAnnotation(BodyParam.class, new BodyParamMapper(typeConverter));
        eventManager.registerAnnotation(QueryParam.class, new QueryParamMapper(typeConverter));
        eventManager.registerAnnotation(UrlParam.class, new UrlParamMapper(router));
        eventManager.registerAnnotation(HeaderParam.class, new HeaderParamMapper());
        eventManager.registerAnnotation(Response.class, new BodyResponseMapper(typeConverter));
        eventManager.registerAnnotation(Route.class, new RoutePredicate(router));

        eventManager.registerListener(new EventListener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void handle(PageRequestEvent e) {
                logger.atInfo().log("Served %s in %.4f ms", e.getPath(), (System.nanoTime() - e.getCreationTime()) / 1.e6);
            }
        });

        eventManager.registerListener(new WebSocketListener());

        registerConnectionHandler(new HttpConnectionHandler());
        registerConnectionHandler(new WebsocketConnectionHandler());

        setDefaultConnectionHandler(HttpConnectionHandler.class);
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void setDefaultConnectionHandler(Class<? extends ConnectionHandler> handler) {
        defaultHandler = handler;
    }

    public void registerConnectionHandler(ConnectionHandler handler) throws IOException {
        Selector clientSelector = Selector.open();

        serverSocketChannel.register(clientSelector, SelectionKey.OP_ACCEPT);
        handler.init(this, eventManager, clientSelector);

        connectionHandlers.put(handler.getClass(), handler);
    }

    public @Nullable ConnectionHandler getConnectionHandler(Class<? extends ConnectionHandler> handler) {
        return connectionHandlers.get(handler);
    }

    public void start() throws IOException {
        for (ConnectionHandler e : connectionHandlers.values()) {
            Thread t = new Thread(e);
            t.setName("Connection Handler: " + e.getClass());
            t.start();
        }

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            int readyCount = selector.select();

            if (readyCount == 0) {
                continue;
            }

            Set<SelectionKey> ready = selector.selectedKeys();
            Iterator<SelectionKey> iterator = ready.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    key.cancel();
                    continue;
                }

                if (key.isAcceptable()) {
                    try {
                        UUID uuid = UUID.randomUUID();
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);

                        connectionHandlers.get(defaultHandler).register(client, uuid, null);
                    } catch (Exception e) {
                        key.cancel();
                    }
                }
            }
        }
    }
}
