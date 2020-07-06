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

package me.infuzion.web.server.http.parser;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.util.ByteBufferUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a String HTTP Request into a {@link HttpRequest}.
 */
public class HttpParser {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @NotNull
    private final Config config;

    public HttpParser() {
        this(new HttpParser.Config());
    }

    public HttpParser(@NotNull HttpParser.Config config) {
        this.config = config;
    }

    protected Map<String, String> parseHeaders(String[] split) {
        Map<String, String> headerMap = new HashMap<>();

        for (String field : split) {
            if (field.isEmpty()) {
                continue;
            }

            // The key value pair; Content-Length, 32
            String[] kv = field.split(":", 2);
            if (kv.length != 2) {
                if (config.strictMode) {
                    throw new ParseException("Invalid header without a key-value pair");
                }
                continue;
            }

            headerMap.put(kv[0].trim().toLowerCase(), kv[1].trim());
        }

        return Collections.unmodifiableMap(headerMap);
    }

    protected HttpRequest parseStatusAndHeaders(String request) {
        String[] lines = request.split("\r\n");
        String[] statusLine = lines[0].split(" ");

        if (statusLine.length != 3) {
            logger.atWarning().log("Received invalid status line: %s", statusLine);
            throw new ParseException("Status line is invalid");
        }

        HttpMethod method = HttpMethod.valueOf(statusLine[0].toUpperCase());

        URI pathQuery;
        try {
            pathQuery = new URI(statusLine[1]);
        } catch (URISyntaxException e) {
            throw new ParseException("Invalid path");
        }
        String path = pathQuery.getPath();
        String query = pathQuery.getQuery() != null ? pathQuery.getQuery() : "";
        String version = statusLine[2];

        int headerEndPosition = request.indexOf("\r\n\r\n");

        Map<String, String> headers;

        headers = parseHeaders(request.substring(request.indexOf("\r\n") + 2, headerEndPosition).split("\r\n"));

        return new HttpRequest(method, path, query, version, headers, null);
    }

    /**
     * Parses a given ByteBuffer containing a HTTP request. If the request is incomplete, null will be returned.
     *
     * @param request A ByteBuffer containing a HTTP request. The current position of the byte buffer must be the end
     *                of the HTTP request.
     * @return An HTTPRequest object containing the information present in `request` or null if incomplete.
     */
    public @Nullable HttpRequest parse(@NotNull ByteBuffer request) {
        int headerEndPosition = ByteBufferUtils.getOffsetToEndOfBoundary(0, request, ByteBufferUtils.CRLFCRLF);

        if (headerEndPosition == -1) {
            return null;
        }

        String r = new String(request.array(), 0, headerEndPosition, StandardCharsets.UTF_8);

        HttpRequest partial = parseStatusAndHeaders(r);

        if (partial.getHeaders().containsKey("content-length")) {
            int contentLength = Integer.parseInt(partial.getHeaders().get("content-length"));

            int oldPos = request.position();

            if (headerEndPosition + contentLength > oldPos) {
                return null;
            }


            request.position(headerEndPosition);

            ByteBuffer bodySlice = request.slice();

            // Set request position to end of body
            request.position(headerEndPosition + contentLength);

            bodySlice.limit(contentLength);

            partial.setBody(bodySlice);
        } else {
            request.position(headerEndPosition);
        }
        return partial;
    }

    public static class Config {
        boolean strictMode = true;

        @Contract("_ -> this")
        Config setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
            return this;
        }
    }

}
