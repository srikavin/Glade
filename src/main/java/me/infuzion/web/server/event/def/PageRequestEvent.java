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

package me.infuzion.web.server.event.def;

import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.RequestEvent;
import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.http.HttpResponse;
import me.infuzion.web.server.http.parser.BodyData;
import me.infuzion.web.server.http.parser.HttpRequest;
import me.infuzion.web.server.util.HttpParameters;
import me.infuzion.web.server.util.Utilities;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Represents an HTTP client requesting a path. This event should be fired when a new request is made to the server.
 */
public class PageRequestEvent extends Event implements RequestEvent {

    private final @NotNull HttpRequest request;

    private final @NotNull BodyData bodyData;

    private final @NotNull HttpParameters queryParams;

    private final @NotNull HttpResponse response;

    public PageRequestEvent(@NotNull HttpRequest request, @NotNull BodyData bodyData) {
        this.request = request;
        this.bodyData = bodyData;
        this.response = new HttpResponse();
        this.queryParams = new HttpParameters(Utilities.parseQuery(request.getQuery()));
    }

    @Override
    public HttpMethod getHttpMethod() {
        return request.getMethod();
    }

    public @NotNull BodyData getBodyData() {
        return bodyData;
    }

    public Map<String, String> getHeaders() {
        return request.getHeaders();
    }

    @Override
    public String getPath() {
        return request.getPath();
    }

    @Override
    public String getRequestData() {
        return request.getBody();
    }

    @Override
    public ByteBuffer getRawRequestData() {
        return request.getRawBody();
    }

    @Override
    public HttpParameters getQueryParameters() {
        return queryParams;
    }

    @Override
    public void setBody(String body) {
        response.setBody(body);
    }

    @Override
    public void setBody(ByteBuffer body) {
        response.setBody(body);
    }

    @Override
    public void setContentType(String type) {
        response.setContentType(type);
    }

    public @NotNull HttpResponse getResponse() {
        return response;
    }
}
