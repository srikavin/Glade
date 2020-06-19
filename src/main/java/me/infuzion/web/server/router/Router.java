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

import me.infuzion.web.server.event.reflect.param.HasPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface Router {
    /**
     * Parses the dynamic segments of the route and returns them.
     * Should return null if the visitedPath is not valid for routePath
     *
     * @param routePath The path expected for the route
     *                  e.g. "/user/:user_id"
     * @param event     The path to parsed to extract dynamic segments from
     *                  e.g. "/user/123"
     * @return A map if visitedPath is valid for routePath or null
     */
    @Nullable Map<String, String> parseDynamicSegments(@NotNull String routePath, @NotNull HasPath event);
}
