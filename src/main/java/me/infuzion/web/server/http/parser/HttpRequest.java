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

import me.infuzion.web.server.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class HttpRequest {
    @NotNull
    private final String version;
    @NotNull
    private final HttpMethod method;
    @NotNull
    private final String path;
    @NotNull
    private final String query;
    @NotNull
    private final Map<String, String> headers;
    private @Nullable ByteBuffer body;

    public HttpRequest(@NotNull HttpMethod method, @NotNull String path, @NotNull String query, @NotNull String version, @NotNull Map<String, String> headers, @Nullable ByteBuffer body) {
        this.query = query;
        this.version = version;
        this.method = method;
        this.path = path;
        this.body = body;

        @NotNull Map<String, String> temp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        temp.putAll(headers);
        this.headers = Collections.unmodifiableMap(temp);
    }

    public @NotNull String getVersion() {
        return version;
    }

    public @NotNull HttpMethod getMethod() {
        return method;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getQuery() {
        return query;
    }

    public @NotNull Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @return A UTF-8 encoded string of the raw body.
     * @deprecated This method returns the raw body encoded to UTF-8. The body may have a different charset, which
     * results in corrupted/invalid data. Instead use the parsed body or use {@link #getRawBody()}
     */
    @Deprecated
    public @Nullable String getBody() {
        if (body == null) {
            return null;
        }

        return StandardCharsets.UTF_8.decode(body).toString();
    }

    void setBody(@Nullable byte[] body) {
        this.body = ByteBuffer.wrap(body);
    }

    void setBody(@Nullable ByteBuffer body) {
        this.body = body;
    }

    public @Nullable ByteBuffer getRawBody() {
        return body;
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "version='" + version + '\'' +
                ", method=" + method +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", hasBody='" + (body != null) + '\'' +
                '}';
    }
}
