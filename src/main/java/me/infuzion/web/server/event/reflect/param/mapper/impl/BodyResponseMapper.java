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
import me.infuzion.web.server.event.reflect.param.CanSetBody;
import me.infuzion.web.server.event.reflect.param.CanSetHeaders;
import me.infuzion.web.server.event.reflect.param.TypeConverter;
import me.infuzion.web.server.event.reflect.param.mapper.ResponseMapper;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class BodyResponseMapper implements ResponseMapper<Response, CanSetBody, Object> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final TypeConverter typeConverter;

    public BodyResponseMapper(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public void map(Response annotation, Method method, CanSetBody event, Object returnValue) {
        if (annotation.raw()) {
            if (returnValue instanceof ByteBuffer) {
                event.setBody((ByteBuffer) returnValue);
            } else if (returnValue instanceof byte[]) {
                event.setBody(ByteBuffer.wrap((byte[]) returnValue));
            } else if (returnValue instanceof String) {
                event.setResponseBody((String) returnValue);
            } else {
                throw new RuntimeException("Raw response must return byte buffer, byte[], or String!");
            }
        }

        event.setResponseBody(typeConverter.serialize(returnValue));
    }

    @Override
    public boolean validate(Response annotation, Method method, Class<?> returnType, Class<? extends Event> event) {
        if (!CanSetHeaders.class.isAssignableFrom(event) && !annotation.value().equals(Response.UNALTERED_CONTENT_TYPE)) {
            logger.atSevere().log("Method does not support setting content type!");
            return false;
        }

        if (annotation.raw()) {
            if (ByteBuffer.class.isAssignableFrom(returnType) || byte[].class.isAssignableFrom(returnType)) {
                return true;
            }

            logger.atSevere().log("Raw response must return byte buffer, byte[], or String!");
            return false;
        }

        return true;
    }
}
