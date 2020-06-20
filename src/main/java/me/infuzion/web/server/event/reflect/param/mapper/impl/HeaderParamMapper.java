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
import me.infuzion.web.server.event.reflect.param.HasHeaders;
import me.infuzion.web.server.event.reflect.param.mapper.ParamMapper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class HeaderParamMapper implements ParamMapper<HeaderParam, HasHeaders, Object> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @Override
    public Object map(HeaderParam annotation, Method method, Class<?> parameterType, HasHeaders event) {
        return event.getRequestHeaders().get(annotation.value());
    }

    @Override
    public boolean validate(HeaderParam annotation, @Nullable Method method, Class<?> parameterType, Class<? extends Event> event) {
        if (!HasHeaders.class.isAssignableFrom(event)) {
            return false;
        }

        logger.atSevere().log("Parameters annotated with @HeaderParam must be Strings");

        return String.class.isAssignableFrom(parameterType);
    }
}
