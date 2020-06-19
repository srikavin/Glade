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
import me.infuzion.web.server.event.reflect.param.HasBody;
import me.infuzion.web.server.event.reflect.param.TypeConverter;
import me.infuzion.web.server.event.reflect.param.mapper.ParamMapper;
import me.infuzion.web.server.http.parser.BodyData;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class BodyParamMapper implements ParamMapper<BodyParam, HasBody, Object> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final TypeConverter typeConverter;

    public BodyParamMapper(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public Object map(BodyParam annotation, @Nullable Method method, Class<?> parameterType, HasBody event) {
        if (annotation.value().equalsIgnoreCase(BodyParam.ENTIRE_BODY)) {
            if (annotation.raw()) {
                if (String.class.isAssignableFrom(parameterType)) {
                    return event.getRequestData();
                }
                return event.getRawRequestData();
            }

            System.out.println(event.getRequestData());

            return typeConverter.deserialize(event.getRequestData(), parameterType);
        }

        BodyData.BodyField field = event.getBodyData().getFields().get(annotation.value());

        if (field == null) {
            return null;
        }

        if (annotation.raw()) {
            if (String.class.isAssignableFrom(parameterType)) {
                return field.getContent();
            }
            return field.getRawContent();
        }


        return typeConverter.deserialize(field.getContent(), parameterType);
    }

    @Override
    public boolean validate(BodyParam annotation, @Nullable Method method, Class<?> parameterType, Class<? extends Event> event) {
        if (annotation.raw()) {
            if (!(ByteBuffer.class.isAssignableFrom(parameterType)) && !(String.class.isAssignableFrom(parameterType))) {
                logger.atSevere().log("BodyParam raw is true, but type of parameter is not ByteBuffer or String!");
                return false;
            }
        }

        if (!HasBody.class.isAssignableFrom(event)) {
            logger.atSevere().log("Event %s is not assignable to HasBody!", event);
            return false;
        }

        return true;
    }
}
