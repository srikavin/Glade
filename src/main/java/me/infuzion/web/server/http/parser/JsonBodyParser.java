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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JsonBodyParser implements BodyParser {
    private final Gson gson = new Gson();

    @Override
    public boolean matches(HttpRequest request, ByteBuffer body) {
        String contentType = request.getHeaders().get("content-type");

        if (contentType == null) {
            return false;
        }

        return contentType.contains("application/json");
    }

    @Override
    public @NotNull BodyData parse(@Nullable HttpRequest request, @NotNull ByteBuffer body) {
        String json = StandardCharsets.UTF_8.decode(body).toString();

        JsonElement object = JsonParser.parseString(json);

        Map<String, BodyData.BodyField> fields = new HashMap<>();

        if (object.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : object.getAsJsonObject().entrySet()) {
                fields.put(e.getKey(),
                        new BodyData.BodyField(e.getKey(), null, StandardCharsets.UTF_8.encode(gson.toJson(e.getValue())),
                                "application/json", StandardCharsets.UTF_8));
            }
        } else {
            return new BodyData(Collections.emptyMap());
        }

        return new BodyData(fields);
    }
}
