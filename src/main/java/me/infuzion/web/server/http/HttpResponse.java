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

package me.infuzion.web.server.http;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains the information necessary to construct a HTTP response to send to a web client.
 */
public class HttpResponse {
    private final Map<String, String> headers = new HashMap<>();
    private int statusCode = 200;
    private @Nullable ByteBuffer body = null;
    private String contentType = "text/html";

    /**
     * The initial status code is 200.
     *
     * @return The current status code of this response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code of this response.
     *
     * @param statusCode The status code to send.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Adds a header to this response. If the header value was previously specified, this method will overwrite the
     * previous value.
     *
     * @param header The name of the header
     * @param value  The value to set the given header to
     */
    public void setHeader(String header, String value) {
        headers.put(header.toLowerCase(), value);
    }

    /**
     * @return A map containing the headers associated with this response. Note that all of the header names have been
     * made lowercase.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Gets the body of this HTTP response. A null value indicates that this response has no associated body.
     *
     * @return A ByteBuffer containing the body of this response or null if it has no body.
     */
    public @Nullable ByteBuffer getBody() {
        return body;
    }

    /**
     * Sets the body of the response to the given byte buffer. The byte buffer should contain the response starting at
     * position zero, and the limit of the buffer should be set to the end of the response.
     * <p>
     * Note that the byte buffer's position will be set to 0 when being written to the client.
     *
     * @param body The byte buffer containing the body of the response.
     */
    public void setBody(@Nullable ByteBuffer body) {
        if (body != null) {
            body.position(0);
        }
        this.body = body;
    }

    /**
     * Sets the body of the response to the given byte array. Prefer {@link #setBody(ByteBuffer)} if possible.
     *
     * @param body The byte array containing the body of this response.
     */
    public void setBody(byte[] body) {
        if (body == null) {
            this.body = null;
            return;
        }
        this.body = ByteBuffer.wrap(body);
    }

    /**
     * Sets the body of HTTP response to the given String. It is assumed that the String is encoded in UTF-8.
     *
     * @param body The String containing the response
     */
    public void setBody(@Nullable String body) {
        if (body == null) {
            this.body = null;
            return;
        }

        this.body = StandardCharsets.UTF_8.encode(body);
    }

    /**
     * @return The current content type of the body of this response
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type of this response
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
