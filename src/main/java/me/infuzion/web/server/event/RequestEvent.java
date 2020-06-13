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

package me.infuzion.web.server.event;

import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.http.parser.BodyData;
import me.infuzion.web.server.util.HttpParameters;

import java.nio.ByteBuffer;

/**
 * Implementing events can be used with dynamic URL-based routing.
 */
public interface RequestEvent {
    HttpMethod getHttpMethod();

    /**
     * @return The relative path for this request.
     */
    String getPath();

    /**
     * @return The data associated with the body of this request.
     */
    String getRequestData();

    ByteBuffer getRawRequestData();

    /**
     * @return The data associated with the query parameters of this request.
     */
    HttpParameters getQueryParameters();

    BodyData getBodyData();

    void setBody(String body);

    void setBody(ByteBuffer body);

    void setContentType(String type);
}
