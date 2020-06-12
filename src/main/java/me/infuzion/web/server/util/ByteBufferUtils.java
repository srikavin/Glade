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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBufferUtils {
    public static final byte[] CRLFCRLF = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    public static int getOffsetToEndOfBoundary(int offset, ByteBuffer data, byte[] boundary) {
        int boundaryOffset = 0;
        while (offset < data.limit()) {
            if (data.get(offset) == boundary[boundaryOffset]) {
                boundaryOffset++;
            } else {
                boundaryOffset = 0;
            }
            offset++;

            if (boundaryOffset == boundary.length) {
                return offset;
            }
        }
        return -1;
    }

    public static int getOffsetToEndOfBoundary(ByteBuffer data, byte[] boundary) {
        return getOffsetToEndOfBoundary(data.position(), data, boundary);
    }

    public static String getDataUntilAsString(ByteBuffer data, byte[] search) {
        int finalOffset = getOffsetToEndOfBoundary(data, search);
        if (finalOffset == -1) {
            return null;
        }
        byte[] strData = new byte[finalOffset - data.position() - search.length];

        data.mark();
        data.get(strData);
        data.reset();

        return new String(strData, 0, strData.length, StandardCharsets.UTF_8);
    }
}
