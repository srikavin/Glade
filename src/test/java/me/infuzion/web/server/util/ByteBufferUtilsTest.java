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

package me.infuzion.web.server.util;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufferUtilsTest {

    @Test
    void getOffsetToEndOfBoundary() {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode("TEST \r\n\r\n");

        byte[] boundary = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        int offset = ByteBufferUtils.getOffsetToEndOfBoundary(buffer, boundary);

        assertEquals(9, offset);
    }

    @Test
    void getOffsetToEndOfBoundaryUnicode() {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode("À϶￥\uD83C\uDF2D∮⇒⇔¬β∀₂⌀ıəˈ⍳⍴V)BOUNDARY═€ίζησθლბშიнстемองจึองታሽ።ደለᚢᛞᚦᚹ⠳⠞⠊⠎▉▒▒▓\uD83D\uDE03");

        byte[] boundary = "BOUNDARY═€ί".getBytes(StandardCharsets.UTF_8);

        int offset = ByteBufferUtils.getOffsetToEndOfBoundary(buffer, boundary);

        assertEquals(64, offset);
    }

    @Test
    void getDataUntilAsString() {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode("TEST \r\n\r\n");

        byte[] boundary = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        assertEquals("TEST ", ByteBufferUtils.getDataUntilAsString(buffer, boundary));
    }
}