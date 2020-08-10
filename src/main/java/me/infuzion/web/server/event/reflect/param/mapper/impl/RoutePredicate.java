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

package me.infuzion.web.server.event.reflect.param.mapper.impl;

import com.google.common.flogger.FluentLogger;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.reflect.Route;
import me.infuzion.web.server.event.reflect.param.HasHttpMethod;
import me.infuzion.web.server.event.reflect.param.HasPath;
import me.infuzion.web.server.event.reflect.param.mapper.EventPredicate;
import me.infuzion.web.server.router.RouteMethod;
import me.infuzion.web.server.router.Router;

import java.util.Arrays;
import java.util.Map;

public class RoutePredicate implements EventPredicate<Route, HasPath> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Router router;

    public RoutePredicate(Router router) {
        this.router = router;
    }

    @Override
    public boolean shouldCall(Route annotation, HasPath event) {
        Map<String, String> val = router.parseDynamicSegments(annotation.value(), event);

        if (event instanceof HasHttpMethod) {
            boolean hasMethod = Arrays.stream(annotation.methods())
                    .anyMatch(e -> e == RouteMethod.ANY || e.toHttpMethod() == ((HasHttpMethod) event).getHttpMethod());

            if (!hasMethod) {
                return false;
            }
        }

        return val != null;
    }

    @Override
    public int executionOrder() {
        return -10;
    }

    @Override
    public boolean validate(Route annotation, Class<? extends Event> event) {
        return HasPath.class.isAssignableFrom(event);
    }
}
