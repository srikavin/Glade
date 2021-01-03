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
import me.infuzion.web.server.http.HttpResponse;
import me.infuzion.web.server.performance.PerformanceMetrics;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DefaultResponseGenerator implements ResponseGenerator {
    @Override
    public ByteBuffer generateResponse(Event event) {
        if (!(event instanceof PageRequestEvent)) {
            return null;
        }

        long lastRequestTime = event.getCreationTime();

        PageRequestEvent requestEvent = (PageRequestEvent) event;
        HttpResponse response = requestEvent.getResponse();

        int status = response.getStatusCode();
        String contentType = response.getContentType();
        ByteBuffer rawResponse = response.getBody();
        Map<String, String> headers = response.getHeaders();

        StringBuilder generated = new StringBuilder(256);

        generated.append("HTTP/1.1 ").append(status).append("\r\n");

        writeHeaderLine(generated, "Content-Type", contentType);
        writeHeaderLine(generated, "Content-Length", (rawResponse != null ? rawResponse.limit() : 0));
        writeHeaderLine(generated, "Connection", "Keep-Alive");
        writeHeaderLine(generated, "Keep-Alive", "timeout=5, max=1000");
        writeHeaderLine(generated, "Server", "Glade v" + Server.version);
        writeHeaderLine(generated, "Server-Timing", PerformanceMetrics.generateServerTimingHeader());
        writeHeaders(generated, headers);

        long elapsedTime = System.nanoTime() - lastRequestTime;
        writeLastHeaderLine(generated, "X-Request-Time", elapsedTime + "ns");

        return StandardCharsets.UTF_8.encode(generated.toString());
    }

    @Override
    public boolean shouldCopyBody(Event event) {
        return true;
    }

    protected void writeHeaders(StringBuilder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            writeHeaderLine(builder, e.getKey(), e.getValue());
        }
    }

    protected StringBuilder writeLastHeaderLine(StringBuilder builder, String key, Object value) {
        return writeHeaderLine(builder, key, value).append("\r\n");
    }

    protected StringBuilder writeHeaderLine(StringBuilder builder, String key, Object value) {
        return builder.append(key).append(": ").append(value).append("\r\n");
    }
}
