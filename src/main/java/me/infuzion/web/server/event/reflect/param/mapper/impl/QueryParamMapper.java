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
import me.infuzion.web.server.event.reflect.param.HasQueryParameters;
import me.infuzion.web.server.event.reflect.param.TypeConverter;
import me.infuzion.web.server.event.reflect.param.mapper.ParamMapper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public class QueryParamMapper implements ParamMapper<QueryParam, HasQueryParameters, Object> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final TypeConverter typeConverter;

    public QueryParamMapper(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public Object map(QueryParam annotation, @Nullable Method method, Class<?> parameterType, HasQueryParameters event) {
        String queryId = annotation.value();

        if (parameterType.isArray()) {
            String[] arr = event.getQueryParameters().get(queryId).toArray(new String[]{});

            Object[] deserialized = new Object[arr.length];

            for (int i = 0; i < arr.length; i++) {
                String queryElement = arr[i];
                deserialized[i] = this.typeConverter.deserialize(queryElement, parameterType.getComponentType());
            }

            return deserialized;
        }

        String val = null;
        List<String> queryParams = event.getQueryParameters().get(queryId);
        if (queryParams.size() > 0) {
            val = queryParams.get(queryParams.size() - 1);
        }

        return this.typeConverter.deserialize(val, parameterType);
    }

    @Override
    public boolean validate(QueryParam annotation, @Nullable Method method, Class<?> parameterType, Class<? extends Event> event) {
        return HasQueryParameters.class.isAssignableFrom(event);
    }
}
