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

import me.infuzion.web.server.event.AbstractEvent;
import me.infuzion.web.server.event.Event;
import me.infuzion.web.server.event.reflect.param.DefaultTypeConverter;
import me.infuzion.web.server.event.reflect.param.HasQueryParameters;
import me.infuzion.web.server.util.HttpParameters;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryParamMapperTest {
    private static class TestEvent extends AbstractEvent implements HasQueryParameters {
        HttpParameters parameters;

        @Override
        public HttpParameters getQueryParameters() {
            return parameters;
        }
    }

    @Test
    void mapQueryArray() {
        TestEvent event = new TestEvent();
        event.parameters = new HttpParameters(Map.of("q1", List.of("12", "13", "14", "15")));

        QueryParam queryParamQ1 = new QueryParam() {
            @Override
            public String value() {
                return "q1";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return QueryParam.class;
            }
        };

        QueryParam queryParamQ3 = new QueryParam() {
            @Override
            public String value() {
                return "q3";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return QueryParam.class;
            }
        };


        QueryParamMapper mapper = new QueryParamMapper(new DefaultTypeConverter());

        Object[] obj = (Object[]) mapper.map(queryParamQ1, null, String[].class, event);

        assertEquals(obj[0], "12");
        assertEquals(obj[1], "13");
        assertEquals(obj[2], "14");
        assertEquals(obj[3], "15");

        Object[] obj1 = (Object[]) mapper.map(queryParamQ1, null, int[].class, event);
        assertEquals(obj1[0], 12);
        assertEquals(obj1[1], 13);
        assertEquals(obj1[2], 14);
        assertEquals(obj1[3], 15);

        Object obj2 = mapper.map(queryParamQ3, null, int[].class, event);
        assertEquals(0, ((Object[]) obj2).length);
    }

    @Test
    void mapQuery() {
        TestEvent event = new TestEvent();
        event.parameters = new HttpParameters(Map.of(
                "q2", List.of("12", "13", "14", "15"),
                "q4", List.of("asd"))
        );

        QueryParam queryParamQ2 = new QueryParam() {
            @Override
            public String value() {
                return "q2";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return QueryParam.class;
            }
        };

        QueryParam queryParamQ4 = new QueryParam() {
            @Override
            public String value() {
                return "q4";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return QueryParam.class;
            }
        };


        QueryParamMapper mapper = new QueryParamMapper(new DefaultTypeConverter());

        String obj = (String) mapper.map(queryParamQ2, null, String.class, event);
        assertEquals("15", obj);

        int obj2 = (int) mapper.map(queryParamQ2, null, int.class, event);
        assertEquals(15, obj2);

        String obj3 = (String) mapper.map(queryParamQ4, null, String.class, event);
        assertEquals("asd", obj3);

        assertTrue(mapper.validate(queryParamQ2, null, Object.class, TestEvent.class));
        assertFalse(mapper.validate(queryParamQ2, null, Object.class, Event.class));
    }
}