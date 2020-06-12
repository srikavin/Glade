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

package me.infuzion.web.server.router.def;

import me.infuzion.web.server.event.RequestEvent;
import me.infuzion.web.server.router.Router;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DefaultRouter implements Router<RequestEvent> {
    @Override
    public @Nullable Map<String, String> parseDynamicSegments(@NotNull String routePath, @NotNull RequestEvent event) {
        // "/user/:user_id"
        // "/user/asdbawe"

        if (routePath.equalsIgnoreCase("*")) {
            return new HashMap<>();
        }

        String[] routePathSplit = routePath.split("/");
        String[] visitedPathSplit = event.getPath().split("/");

        Map<String, String> toRet = new HashMap<>();

        if (routePathSplit.length != visitedPathSplit.length) {
            return null;
        }

        for (int i = 0; i < routePathSplit.length; i++) {
            String curRoute = routePathSplit[i];
            String curVisited = visitedPathSplit[i];

            if (curRoute.startsWith(":")) {
                String curDynamicSegment = curRoute.substring(1);
                toRet.put(curDynamicSegment, curVisited);
            } else if (!curRoute.equalsIgnoreCase(curVisited)) {
                return null;
            }

        }

        return toRet;
    }
}
