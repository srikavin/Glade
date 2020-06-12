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

package me.infuzion.web.server.event.reflect.param;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class DefaultTypeConverter implements TypeConverter {
    private static final Gson gson = new Gson();

    @Override
    public <T> T deserialize(String content, Class<T> type) {
        if (content == null) {
            return null;
        }

        try {
            return gson.fromJson(content, type);
        } catch (JsonSyntaxException e) {
            if (type.isAssignableFrom(String.class)) {
                //noinspection unchecked
                return (T) content;
            }
            throw e;
        }
    }

    @Override
    public String serialize(Object content) {
        if (content instanceof String) {
            return (String) content;
        }
        return gson.toJson(content);
    }
}
