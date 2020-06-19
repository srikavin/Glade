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

package me.infuzion.web.server.response;

import me.infuzion.web.server.Server;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.def.PageRequestEvent;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class WebSocketResponseGenerator extends DefaultResponseGenerator {
    @Override
    public ByteBuffer generateResponse(Event event) {
        if (!(event instanceof PageRequestEvent)) {
            return null;
        }
        PageRequestEvent pEvent = (PageRequestEvent) event;

        StringBuilder generated = new StringBuilder(256);

        generated.append("HTTP/1.1 101 Switching Protocols\r\n");
        writeHeaderLine(generated, "Upgrade", "websocket");
        writeHeaderLine(generated, "Connection", "Upgrade");
        writeHeaders(generated, pEvent.getResponse().getHeaders());
        writeLastHeaderLine(generated, "Server", "Glade v" + Server.version);

        return ByteBuffer.wrap(generated.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean shouldCopyBody(Event event) {
        // ignore the body generated as the response would be treated as a websocket frame
        return false;
    }
}
