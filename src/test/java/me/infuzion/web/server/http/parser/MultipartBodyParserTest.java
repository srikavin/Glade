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

class MultipartBodyParserTest {

    @Test
    void parse() {
        MultipartBodyParser parser = new MultipartBodyParser();
        HttpParser httpParser = new HttpParser();

        // From https://stackoverflow.com/questions/4238809/example-of-multipart-form-data
        String request = "POST / HTTP/1.1\r\n" +
                "Host: localhost:8000\r\n" +
                "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
                "Accept-Language: en-US,en;q=0.5\r\n" +
                "Accept-Encoding: gzip, deflate\r\n" +
                "Cookie: __atuvc=34%7C7; permanent=0; _gitlab_session=226ad8a0be43681acf38c2fab9497240; __profilin=p%3Dt; request_method=GET\r\n" +
                "Connection: keep-alive\r\n" +
                "Content-Type: multipart/form-data; boundary=---------------------------9051914041544843365972754266\r\n" +
                "Content-Length: 554\r\n" +
                "\r\n" +
                "-----------------------------9051914041544843365972754266\r\n" +
                "Content-Disposition: form-data; name=\"text\"\r\n" +
                "\r\n" +
                "text default\r\n" +
                "-----------------------------9051914041544843365972754266\r\n" +
                "Content-Disposition: form-data; name=\"file1\"; filename=\"a.txt\"\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Content of a.txt.\r\n" +
                "\r\n" +
                "-----------------------------9051914041544843365972754266\r\n" +
                "Content-Disposition: form-data; name=\"file2\"; filename=\"a.html\"\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<!DOCTYPE html><title>Content of a.html.</title>\r\n" +
                "\r\n" +
                "-----------------------------9051914041544843365972754266--";

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(request);
        buffer.position(buffer.limit());

        HttpRequest ret = httpParser.parse(buffer);

        assertNotNull(ret);
        assertNotNull(ret.getRawBody());
        BodyData data = parser.parse(ret, ret.getRawBody());

        assertTrue(parser.matches(ret, ret.getRawBody()));

        BodyData.BodyField text = data.getFields().get("text");
        assertEquals(text.getContent(), "text default");
        assertEquals(text.getContentType(), "text/plain");
        assertNull(text.getFileName());
        assertEquals(text.getEncoding(), StandardCharsets.UTF_8);
        assertEquals(text.getRawContent().position(0), StandardCharsets.UTF_8.encode("text default"));
    }

    @Test
    void parse2() {
        MultipartBodyParser parser = new MultipartBodyParser();
        HttpParser httpParser = new HttpParser();
        // From https://stackoverflow.com/questions/4238809/example-of-multipart-form-data
        String request = "POST / HTTP/1.1\r\n" +
                "HOST: host.example.com\r\n" +
                "Cookie: some_cookies...\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Content-Type: multipart/form-data; boundary=12345\r\n" +
                "Content-Length: 417\r\n" +
                "\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"sometext\"\r\n" +
                "\r\n" +
                "some text that you wrote in your html form ...\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"name_of_post_request\";filename=\"filename.xyz\"\r\n" +
                "\r\n" +
                "content of filename.xyz that you upload in your form with input[type=file]\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"image\";filename=\"picture_of_sunset.jpg\"\r\n" +
                "\r\n" +
                "content of picture_of_sunset.jpg ...\r\n" +
                "--12345--";

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(request);
        buffer.position(buffer.limit());

        HttpRequest ret = httpParser.parse(buffer);

        assertNotNull(ret);
        assertNotNull(ret.getRawBody());

        parser.parse(ret, ret.getRawBody());
    }

    @Test
    void parseWithException() {
        MultipartBodyParser parser = new MultipartBodyParser();
        HttpParser httpParser = new HttpParser();
        // From https://stackoverflow.com/questions/4238809/example-of-multipart-form-data


        String request = "POST / HTTP/1.1\r\n" +
                "HOST: host.example.com\r\n" +
                "Cookie: some_cookies...\r\n" +
                "Connection: Keep-Alive\r\n" +
                "Content-Type: multipart/form-data;\r\n" +
                "Content-Length: 417\r\n" +
                "\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"sometext\"\r\n" +
                "\r\n" +
                "some text that you wrote in your html form ...\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"name_of_post_request\";filename=\"filename.xyz\"\r\n" +
                "\r\n" +
                "content of filename.xyz that you upload in your form with input[type=file]\r\n" +
                "--12345\r\n" +
                "Content-Disposition: form-data; name=\"image\";filename=\"picture_of_sunset.jpg\"\r\n" +
                "\r\n" +
                "content of picture_of_sunset.jpg ...\r\n" +
                "--12345--";

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(request);
        buffer.position(buffer.limit());

        HttpRequest ret = httpParser.parse(buffer);
        assertNotNull(ret);
        assertNotNull(ret.getRawBody());

        assertThrows(ParseException.class, () -> parser.parse(ret, ret.getRawBody()));
    }
}