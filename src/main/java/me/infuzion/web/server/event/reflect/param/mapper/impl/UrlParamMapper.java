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
import me.infuzion.web.server.event.reflect.param.HasPath;
import me.infuzion.web.server.event.reflect.param.mapper.ParamMapper;
import me.infuzion.web.server.router.Router;

import java.lang.reflect.Method;
import java.util.Map;

public class UrlParamMapper implements ParamMapper<UrlParam, HasPath, Object> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Router router;

    public UrlParamMapper(Router router) {
        this.router = router;
    }

    @Override
    public Object map(UrlParam annotation, Method method, Class<?> parameterType, HasPath event) {
        Route route = method.getAnnotation(Route.class);
        String pathId = annotation.value();

        Map<String, String> params = router.parseDynamicSegments(route.value(), event);

        if (params == null) {
            return null;
        }

        return params.get(pathId);
    }

    @Override
    public boolean validate(UrlParam annotation, Method method, Class<?> parameterType, Class<? extends Event> event) {
        Route route = method.getAnnotation(Route.class);

        return HasPath.class.isAssignableFrom(event) && route != null;
    }
}
