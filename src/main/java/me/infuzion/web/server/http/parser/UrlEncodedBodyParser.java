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

import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UrlEncodedBodyParser implements BodyParser {

    @Override
    public boolean matches(HttpRequest request, ByteBuffer body) {
        String contentType = request.getHeaders().get("content-type");

        if (contentType == null) {
            return false;
        }


        return contentType.contains("application/x-www-form-urlencoded");
    }

    @Override
    public @NotNull BodyData parse(@NotNull HttpRequest request, @NotNull ByteBuffer body) {
        String query = StandardCharsets.UTF_8.decode(body).toString();

        String[] split = query.split("&");

        Map<String, BodyData.BodyField> fields = new HashMap<>();

        for (String e : split) {
            String[] kv = e.split("=");
            if (kv.length != 2) {
                continue;
            }

            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);

            fields.put(k, new BodyData.BodyField(k, null, StandardCharsets.UTF_8.encode(v), "text/plain", StandardCharsets.UTF_8));
        }

        return new BodyData(fields);
    }
}
