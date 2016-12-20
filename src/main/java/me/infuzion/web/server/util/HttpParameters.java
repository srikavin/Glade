/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpParameters implements Iterable<Map.Entry<String, List<String>>> {
    private final String method;
    private Map<String, List<String>> parameters = new HashMap<>();
    private boolean initialized = false;

    public HttpParameters(String method) {
        this.method = method;
    }

    public void init(Map<String, List<String>> map) {
        if (initialized) {
            return;
        }

        initialized = true;
        this.parameters = map;
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public List<String> get(String name){
        return parameters.get(name);
    }

    @Override
    public Iterator<Map.Entry<String, List<String>>> iterator() {
        return parameters.entrySet().iterator();
    }

    public String getMethod() {
        return method;
    }
}
