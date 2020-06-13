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

package me.infuzion.web.server.event.reflect.param;

import me.infuzion.web.server.event.RequestEvent;
import me.infuzion.web.server.event.def.PageRequestEvent;
import me.infuzion.web.server.event.reflect.Route;
import me.infuzion.web.server.http.HttpMethod;
import me.infuzion.web.server.http.parser.BodyData;
import me.infuzion.web.server.util.HttpParameters;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParameterGeneratorTest {

    @Route("/asd/:asd")
    public void example(PageRequestEvent event, @BodyParam MockClass param, @URLParam("asd") String asd,
                        @QueryParam("name") String name, @QueryParam("data") String data, @QueryParam("multiple") String[] m) {
    }

    @Test
    void generateMethodParameters() throws NoSuchMethodException {
        Method m = this.getClass().getMethod("example", PageRequestEvent.class, MockClass.class, String.class, String.class, String.class, String[].class);

        ParameterGenerator p = new ParameterGenerator(new DefaultTypeConverter(), m);


        RequestEvent mock = new MockRequestEvent();

        Object[] generated = p.generateMethodParameters(mock, Map.of("asd", "name"));

        assertEquals(mock, generated[0]);
        assertTrue(generated[1] instanceof MockClass);
        assertEquals("value", ((MockClass) generated[1]).test);
        assertEquals("name", generated[2]);
        assertNull(generated[3]);
        assertEquals("d1", generated[4]);
        assertArrayEquals(new String[]{"m1", "m2", "m3"}, (Object[]) generated[5]);
    }

    static class MockClass {
        String test;
    }

    static class MockRequestEvent implements RequestEvent {
        @Override
        public HttpMethod getHttpMethod() {
            return HttpMethod.GET;
        }

        @Override
        public String getPath() {
            return "/asd/ksd";
        }

        @Override
        public String getRequestData() {
            return "{\"test\": \"value\"}";
        }

        @Override
        public ByteBuffer getRawRequestData() {
            return ByteBuffer.allocate(16);
        }

        @Override
        public HttpParameters getQueryParameters() {
            return new HttpParameters(Map.of("data", Collections.singletonList("d1"), "multiple", Arrays.asList("m1", "m2", "m3")));
        }

        @Override
        public BodyData getBodyData() {
            return null;
        }

        @Override
        public void setBody(String body) {

        }

        @Override
        public void setBody(ByteBuffer body) {

        }

        @Override
        public void setContentType(String type) {

        }
    }
}