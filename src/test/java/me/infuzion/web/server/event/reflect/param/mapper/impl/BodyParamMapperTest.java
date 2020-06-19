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

import me.infuzion.web.server.EventListener;
import me.infuzion.web.server.event.AbstractEvent;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.reflect.EventHandler;
import me.infuzion.web.server.event.reflect.param.DefaultTypeConverter;
import me.infuzion.web.server.event.reflect.param.HasBody;
import me.infuzion.web.server.http.parser.BodyData;
import me.infuzion.web.server.http.parser.JsonBodyParser;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class BodyParamMapperTest {
    static class TestEvent extends AbstractEvent implements HasBody {
        private final ByteBuffer buffer;
        private final String string;
        private final BodyData bodyData;

        public TestEvent(ByteBuffer buffer, String string, BodyData bodyData) {
            this.buffer = buffer;
            this.string = string;
            this.bodyData = bodyData;
        }

        @Override
        public String getRequestData() {
            return string;
        }

        @Override
        public ByteBuffer getRawRequestData() {
            return buffer;
        }

        @Override
        public BodyData getBodyData() {
            return bodyData;
        }
    }

    @Test
    void rawResponseEntireBody() {
        String str = "test string 123";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(str);

        TestEvent event = new TestEvent(buffer, str, new BodyData(Collections.emptyMap()));

        BodyParamMapper mapper = new BodyParamMapper(new DefaultTypeConverter());

        BodyParam annotation = new BodyParam() {
            @Override
            public String value() {
                return BodyParam.ENTIRE_BODY;
            }

            @Override
            public boolean raw() {
                return true;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BodyParam.class;
            }
        };

        Object testListener = new EventListener() {
            @EventHandler
            public void event1(@BodyParam(raw = true) ByteBuffer body) {

            }

            @EventHandler
            public void event2(@BodyParam(raw = true) String body) {

            }
        };

        Object mapped = mapper.map(annotation, null, ByteBuffer.class, event);
        assertEquals(buffer, mapped);

        mapped = mapper.map(annotation, null, String.class, event);
        assertEquals(str, mapped);
    }

    @Test
    void rawResponseArgument() {
        String str = "{\"value1\": 123}";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(str);

        BodyData bodyData = new JsonBodyParser().parse(null, buffer);

        BodyParam annotation = new BodyParam() {
            @Override
            public String value() {
                return "value1";
            }

            @Override
            public boolean raw() {
                return true;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BodyParam.class;
            }
        };

        BodyParam annotationNull = new BodyParam() {
            @Override
            public String value() {
                return "value4";
            }

            @Override
            public boolean raw() {
                return true;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BodyParam.class;
            }
        };

        TestEvent event = new TestEvent(buffer, str, bodyData);
        BodyParamMapper mapper = new BodyParamMapper(new DefaultTypeConverter());

        Object mapped = mapper.map(annotation, null, ByteBuffer.class, event);
        assertEquals(bodyData.getFields().get("value1").getRawContent(), mapped);

        mapped = mapper.map(annotation, null, String.class, event);
        assertEquals("123", mapped);

        assertTrue(mapper.validate(annotation, null, ByteBuffer.class, TestEvent.class));
        assertTrue(mapper.validate(annotation, null, String.class, TestEvent.class));

        assertFalse(mapper.validate(annotation, null, Object.class, TestEvent.class));
        assertFalse(mapper.validate(annotation, null, ByteBuffer.class, Event.class));

        mapped = mapper.map(annotationNull, null, String.class, event);
        assertNull(mapped);

        mapped = mapper.map(annotationNull, null, ByteBuffer.class, event);
        assertNull(mapped);
    }

    static class TestSchema {
        int value1;
        String value2;
    }

    @Test
    void deserializeArgument() {
        String str = "{\"value1\": 123, \"value2\": \"asd123\"}";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(str);

        BodyData bodyData = new JsonBodyParser().parse(null, buffer);

        BodyParam annotation = new BodyParam() {
            @Override
            public String value() {
                return "value1";
            }

            @Override
            public boolean raw() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BodyParam.class;
            }
        };

        BodyParam fullAnnotation = new BodyParam() {
            @Override
            public String value() {
                return BodyParam.ENTIRE_BODY;
            }

            @Override
            public boolean raw() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return BodyParam.class;
            }
        };


        TestEvent event = new TestEvent(buffer, str, bodyData);
        BodyParamMapper mapper = new BodyParamMapper(new DefaultTypeConverter());

        Object mapped = mapper.map(annotation, null, int.class, event);
        assertEquals(123, mapped);

        mapped = mapper.map(fullAnnotation, null, TestSchema.class, event);
        assertEquals(123, ((TestSchema) mapped).value1);
        assertEquals("asd123", ((TestSchema) mapped).value2);
    }
}