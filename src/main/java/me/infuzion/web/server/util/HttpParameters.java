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

package me.infuzion.web.server.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpParameters implements Iterable<Map.Entry<String, List<String>>> {
    private final Map<String, List<String>> parameters;

    public HttpParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    public boolean contains(String key) {
        return parameters.containsKey(key) && get(key).size() > 0;
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public List<String> get(String name) {
        return parameters.getOrDefault(name, new ArrayList<>());
    }

    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, List<String>>> iterator() {
        return parameters.entrySet().iterator();
    }
}
