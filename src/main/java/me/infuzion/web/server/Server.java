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
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        session = new HashMap<>();
    }

    public void init() {
        initialized = true;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Map<UUID, Map<String, Object>> getSession() {
        return session;
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
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onRequest(Socket client) throws IOException {
        InputStream in = client.getInputStream();
//        BufferedInputStream in = new BufferedInputStream(client.getInputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int amountRead;
        byte[] bytes;

        do {
            bytes = new byte[1024];
            amountRead = in.read(bytes);
            out.write(bytes);
        } while (amountRead > 0 && in.available() > 0);

        byte[] rawBytes = out.toByteArray();
        String rawStr = new String(rawBytes);
        int contStart = rawStr.indexOf("\r\n\r\n");
        if (contStart != -1) {
            contStart += 4;
        }

        BufferedReader reader = new BufferedReader(new StringReader(rawStr));

        try {
            int contentLength = -1;
            String page = "";
            String hostName = "";
            UUID sessionUuid = null;
            HTTPMethod method = null;
            StringBuilder headers = new StringBuilder();
            int lineNumber = 0;
            while (true) {
                lineNumber++;
                final String line = reader.readLine();
                if (line == null) {
                    return;
                }
                headers.append(line).append("\r\n");
                final String contentLengthStr = "Content-Length: ";
                final String hostStr = "Host: ";
                final String cookieStr = "Cookie: ";
                if (line.startsWith(contentLengthStr)) {
                    contentLength = Integer.parseInt(line.substring(contentLengthStr.length()));
                    continue;
                }
                if (line.startsWith(hostStr)) {
                    hostName = line.substring(hostStr.length());
                    continue;
                }

                if (line.startsWith(cookieStr)) {
                    String temp = line.substring(cookieStr.length());
                    String[] cookies = temp.split(";");
                    for (String e : cookies) {
                        String[] cookie = e.split("=", 2);
                        if (cookie[0].trim().equals("session")) {
                            try {
                                UUID tempuuid = UUID.fromString(cookie[1]);
                                session.computeIfAbsent(tempuuid,
                                        id -> session.put(id, new HashMap<>()));
                                sessionUuid = tempuuid;
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    new Cookies(temp);
                }

                if (lineNumber == 1) {
                    String[] split = line.split(" ");
                    method = HTTPMethod.valueOf(split[0].toUpperCase());
                    page = split[1];
                    continue;
                }

                if (lineNumber > 1 && method == null) {
                    return;
                }

                if (line.length() == 0) {
                    break;
                }
            }

            byte[] raw = null;
            if (contentLength > 0) {
                raw = new byte[contentLength];
                System.arraycopy(rawBytes, contStart, raw, 0, contentLength);
            }

            boolean setCookie = false;
            if (sessionUuid == null) {
                setCookie = true;
                sessionUuid = UUID.randomUUID();
                session.put(sessionUuid, new HashMap<>());
            }

            String contentString = raw != null ? new String(raw, StandardCharsets.ISO_8859_1) : "";

            PageRequestEvent event = new PageRequestEvent(page, contentString, hostName,
                    headers.toString(),
                    sessionUuid, session.get(sessionUuid), method, raw);

            if (setCookie) {
                event.addHeader("Set-Cookie", "session=" + sessionUuid);
            }
//            Thread.currentThread().setName("Request for: " + event.getPage());

            event.setResponseGenerator(new DefaultResponseGenerator());
            eventManager.fireEvent(event);

            MimeType mimeType = mimeTypes.getByExtension(event.getContentType());

            if (mimeType != null) {
                event.setContentType(mimeType.getMimeType());
            }

            event.getResponseGenerator().generateResponse(client, event);
            System.out.println(
                    "Request received from: " + client.getInetAddress() + ":" + client.getPort() + " - " +
                            event.getStatusCode() + " " + (System.currentTimeMillis() - lastRequestTime)
                            + "ms " + event.getPage());
        } catch (Exception e) {
            e.printStackTrace();

            String response = Utilities.convertStreamToString(getClass().getResourceAsStream("/web/error/500.html"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            writer.append("HTTP/1.1 500 Server Error\r\n")
                    .append("Content-Length: " + response.getBytes("UTF-8").length + "\r\n\n")
                    .append(response);
            writer.close();
        }
    }
}
