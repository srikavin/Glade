/*
 *    Copyright 2016 Infuzion
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package me.infuzion.web.server.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilitiesTest {
    @Test
    void convertStreamToString() throws UnsupportedEncodingException {
        Utilities utilities = new Utilities();
        String test1In = "This is a test string.";
        String test1Out = Utilities.convertStreamToString(stringToStream(test1In));
        assertTrue(test1In.equals(test1Out));
    }

    private InputStream stringToStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.getBytes("utf-8"));
    }

    @Test
    void splitQuery() throws MalformedURLException, UnsupportedEncodingException {
        URL input = new URL("http://www.example.com");
        assertTrue(Utilities.splitQuery(input).isEmpty());

        URL input2 = new URL("http://www.example.com/?a=123&b=789&key=value");
        Map<String, List<String>> output2 = Utilities.splitQuery(input2);
        assertEquals(3, output2.size());
        assertEquals(1, output2.get("a").size());
        assertEquals("123", output2.get("a").get(0));
    }

    @Test
    void parseQuery() throws UnsupportedEncodingException {
        assertEquals(0, Utilities.parseQuery(null).size());
    }

}