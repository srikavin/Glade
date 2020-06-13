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

package me.infuzion.web.server.router;

import me.infuzion.web.server.http.HttpMethod;

import java.util.HashSet;
import java.util.Set;

public class EventRoute {
    private final String path;
    private final Set<HttpMethod> methods;

    public EventRoute(String path, RouteMethod... methods) {
        Set<HttpMethod> set;
        this.path = path;
        set = new HashSet<>();

        for (RouteMethod e : methods) {
            if (e.equals(RouteMethod.ANY)) {
                this.methods = Set.of(HttpMethod.values());
                return;
            }

            set.add(e.toHttpMethod());
        }
        this.methods = set;
    }

    public Set<HttpMethod> getMethods() {
        return methods;
    }

    public String getPath() {
        return path;
    }
}
