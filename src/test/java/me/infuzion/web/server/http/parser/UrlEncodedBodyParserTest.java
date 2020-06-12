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

package me.infuzion.web.server.http.parser;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UrlEncodedBodyParserTest {

    @Test
    void parse() {
        BodyParser parser = new UrlEncodedBodyParser();

        HttpParser httpParser = new HttpParser();
        String request = "POST / HTTP/1.1\r\n" +
                "HOST: host.example.com\r\n" +
                "Cookie: some_cookies...\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "Content-Length: 38\r\n" +
                "\r\n" +
                "username=example&password=Pa%24%24w0rd";

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(request);
        buffer.position(buffer.limit());

        HttpRequest ret = httpParser.parse(buffer);

        assertNotNull(ret);
        assertNotNull(ret.getRawBody());

        assertTrue(parser.matches(ret, ret.getRawBody()));
        BodyData data = parser.parse(ret, ret.getRawBody());

        BodyData.BodyField username = data.getFields().get("username");
        BodyData.BodyField password = data.getFields().get("password");

        assertNotNull(username);
        assertNotNull(password);

        assertEquals(username.getFieldName(), "username");
        assertEquals(username.getContent(), "example");

        assertEquals(password.getFieldName(), "password");
        assertEquals(password.getContent(), "Pa$$w0rd");
    }
}