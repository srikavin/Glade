/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server;

import com.github.amr.mimetypes.MimeType;
import com.github.amr.mimetypes.MimeTypes;
import me.infuzion.web.server.event.EventManager;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.response.DefaultResponseGenerator;
import me.infuzion.web.server.util.Cookies;
import me.infuzion.web.server.util.HTTPMethod;
import me.infuzion.web.server.util.Utilities;

import java.io.*;
import java.net.HttpCookie;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {

    public static final double version = 0.1;
    private static final MimeTypes mimeTypes = MimeTypes.getInstance();
    private final Map<UUID, Map<String, Object>> session;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean running;
    private boolean initialized;
    private ServerSocket serverSocket;
    private EventManager eventManager;
    private long lastRequestTime = -1;

    public Server(ServerSocket socket) {
        this.serverSocket = socket;
        running = true;
        initialized = false;
        eventManager = new EventManager();
        session = new ConcurrentHashMap<>();
    }

    public void init() {
        initialized = true;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (!initialized) {
                    TimeUnit.MILLISECONDS.sleep(250);
                    continue;
                }
                Socket client = serverSocket.accept();
                lastRequestTime = System.currentTimeMillis();
                executor.submit(() -> {
                    try {
                        onRequest(client);
                    } catch (IOException e) {
                        try {
                            client.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected Map<String, String> parseHeaders(String[] split) {
        //This map allows for case-insensitive keys
        Map<String, String> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


        for (String field : split) {
            field = field.replace("\r\n", "");
            if (field.isEmpty()) {
                continue;
            }

            // The key value pair; Content-Length, 32
            String[] kv = field.split(":", 2);
            if (kv.length != 2) {
                continue;
            }

            headerMap.put(kv[0], kv[1].trim());
        }
        return Collections.unmodifiableMap(headerMap);
    }

    private void onRequest(Socket client) throws IOException {
        InputStream in = client.getInputStream();
//        BufferedInputStream in = new BufferedInputStream(client.getInputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes;

        //Set socket time out to 7.5 seconds
        client.setSoTimeout(7500);

        int headerEndPosition = 0;
        while (headerEndPosition != 4) {
            bytes = new byte[1];
            in.read(bytes);
            out.write(bytes);
            if (headerEndPosition % 2 == 0 && bytes[0] == '\r') {
                headerEndPosition++;
            } else if (headerEndPosition % 2 == 1 && bytes[0] == '\n') {
                headerEndPosition++;
            } else {
                headerEndPosition = 0;
            }
        }

        byte[] rawBytes = out.toByteArray();
        String rawStr = new String(rawBytes);

        String[] split = rawStr.split("\r\n");
        String[] request = split[0].split(" ", 3);
        HTTPMethod method = HTTPMethod.valueOf(request[0]);
        String page = request[1].trim();

        Map<String, String> headers = parseHeaders(split);

        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "-1"));

        byte[] content = null;
        // Read upto 16 MiB
        if (contentLength > 0 && contentLength <= 1024 * 1024 * 16) {
            content = new byte[contentLength];
            int amountRead = 0;
            while (amountRead != contentLength) {
                int tmp = in.read(content, amountRead, content.length);
                if (tmp == -1) {
                    client.close();
                    return;
                }
                amountRead += tmp;
            }
        }

        UUID sessionUuid = null;
        {
            String temp = headers.get("Cookie");
            Cookies cookies = new Cookies(temp);
            HttpCookie sessionCookie = cookies.getCookie("session");
            if (sessionCookie != null) {
                try {
                    UUID tempuuid = UUID.fromString(sessionCookie.getValue());
                    session.putIfAbsent(tempuuid, new ConcurrentHashMap<>());
                    sessionUuid = tempuuid;
                } catch (IllegalArgumentException ignored) {
                }
            }

        }

        try {
            String hostName = "";

            String contentString = content != null ? new String(content, StandardCharsets.ISO_8859_1) : "";

            if (sessionUuid == null) {
                sessionUuid = UUID.randomUUID();
                session.put(sessionUuid, new ConcurrentHashMap<>());
            }
            PageRequestEvent event = new PageRequestEvent(page, contentString, hostName,
                    headers,
                    sessionUuid, session.get(sessionUuid), method, content);

            event.addHeader("Set-Cookie", "session=" + sessionUuid);

            Thread.currentThread().setName("Request for: " + event.getPage());

            event.setResponseGenerator(new DefaultResponseGenerator());
            eventManager.fireEvent(event);

            MimeType mimeType = mimeTypes.getByExtension(event.getContentType());

            if (mimeType != null) {
                event.setContentType(mimeType.getMimeType());
            }

            System.out.println(
                    "Request received from: " + client.getInetAddress() + ":" + client.getPort() + " - " +
                            event.getStatusCode() + " " + (System.currentTimeMillis() - lastRequestTime)
                            + "ms " + event.getPage());
            event.getResponseGenerator().generateResponse(client, event);
        } catch (Exception e) {
            e.printStackTrace();

            String response = Utilities.convertStreamToString(getClass().getResourceAsStream("/web/error/500.html"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            writer.append("HTTP/1.1 500 Server Error\r\n")
                    .append("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length + "\r\n\n")
                    .append(response);
            writer.close();
        }
    }
}
