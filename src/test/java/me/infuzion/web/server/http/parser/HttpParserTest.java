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

import me.infuzion.web.server.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpParserTest {

    @Test
    void parse() {
        HttpParser parser = new HttpParser();

        ByteBuffer buffer = StandardCharsets.UTF_8.encode("GET /test.html HTTP/1.1\r\n" +
                "User-Agent: Mozilla/5.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n" +
                "\r\n");

        buffer.position(buffer.limit());

        HttpRequest ret = parser.parse(buffer);

        assertNotNull(ret);
        assertEquals(HttpMethod.GET, ret.getMethod());
        assertEquals("/test.html", ret.getPath());
        assertEquals("HTTP/1.1", ret.getVersion());
        assertEquals("Mozilla/5.0 (compatible; MSIE5.01; Windows NT)", ret.getHeaders().get("user-agent"));
        assertEquals("en-us", ret.getHeaders().get("accept-language"));
        assertEquals("gzip, deflate", ret.getHeaders().get("accept-encoding"));
        assertEquals("Keep-Alive", ret.getHeaders().get("connection"));
    }

    @Test
    void parseWithBody() {
        HttpParser parser = new HttpParser();

        String body = "Test body \r\n Line 1 \r\n Line 2";

        ByteBuffer buffer = StandardCharsets.UTF_8.encode("POST /test.html?asd=123 HTTP/1.1\r\n" +
                "User-Agent: Mozilla/5.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                "Accept-Language: en-us\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body);

        buffer.position(buffer.limit());

        HttpRequest ret = parser.parse(buffer);

        assertNotNull(ret);

        assertEquals(HttpMethod.POST, ret.getMethod());
        assertEquals("/test.html", ret.getPath());
        assertEquals("HTTP/1.1", ret.getVersion());
        assertEquals("Mozilla/5.0 (compatible; MSIE5.01; Windows NT)", ret.getHeaders().get("user-agent"));
        assertEquals("en-us", ret.getHeaders().get("accept-language"));
        assertEquals("asd=123", ret.getQuery());
        assertEquals("gzip, deflate", ret.getHeaders().get("accept-encoding"));
        assertEquals("Keep-Alive", ret.getHeaders().get("connection"));
        assertEquals(body, ret.getBody());
    }

    @Test
    void parseWithException() {
        HttpParser parser = new HttpParser();

        assertThrows(RuntimeException.class, () -> {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode("GET /test.html\r\n" +
                    "User-Agent: Mozilla/5.0 (compatible; MSIE5.01; Windows NT)\r\n" +
                    "Accept-Language: en-us\r\n" +
                    "Accept-Encoding: gzip, deflate\r\n" +
                    "Connection: Keep-Alive\r\n" +
                    "\r\n");

            buffer.position(buffer.limit());

            parser.parse(buffer);
        });
    }


}