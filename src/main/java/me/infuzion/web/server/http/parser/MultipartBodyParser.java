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

import me.infuzion.web.server.util.ByteBufferUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MultipartBodyParser implements BodyParser {

    private void parseSeparatedValues(Map<String, String> ret, String text) {
        String[] split = text.split(";");
        for (String e : split) {
            String[] kv = e.split("=");
            if (kv.length == 2) {
                String v = kv[1].trim();
                ret.put(kv[0].trim().toLowerCase(), v.substring(1, v.length() - 1));
            }
        }
    }

    private BodyData.BodyField parseChunk(ByteBuffer data, byte[] boundary) {
        // Account for \r\n at end of boundary line
        int offset = ByteBufferUtils.getOffsetToEndOfBoundary(data, boundary) + 2;

        data.position(offset);

        int endOfHeaders = ByteBufferUtils.getOffsetToEndOfBoundary(data, ByteBufferUtils.CRLFCRLF);

        String headers = ByteBufferUtils.getDataUntilAsString(data, ByteBufferUtils.CRLFCRLF);

        if (headers == null) {
            return null;
        }

        String[] headerLine = headers.split("\r\n");

        Map<String, String> map = new HashMap<>();

        for (String header : headerLine) {
            String[] split = header.split(": ");
            if (split.length != 2) {
                continue;
            }

            if (split[0].equalsIgnoreCase("content-type")) {
                map.put("content-type", split[1]);
            }

            parseSeparatedValues(map, split[1]);
        }

        data.position(endOfHeaders);
        // Account for \r\n at end of content
        int endOfData = ByteBufferUtils.getOffsetToEndOfBoundary(data, boundary) - boundary.length - 2;

        ByteBuffer slice = data.slice();
        slice.limit(endOfData - endOfHeaders);

        String name = map.get("name");
        String fileName = map.get("filename");
        String contentType = map.getOrDefault("content-type", "text/plain");
        String encodingStr = map.get("charset");

        Charset encoding = encodingStr == null ? StandardCharsets.UTF_8 : Charset.forName(encodingStr);
        data.position(endOfData);
        return new BodyData.BodyField(name, fileName, slice, contentType, encoding);
    }

    @Override
    public boolean matches(HttpRequest request, ByteBuffer body) {
        return request.getHeaders().get("content-type").contains("multipart/form-data");
    }

    @Override
    public @NotNull BodyData parse(@NotNull HttpRequest request, @NotNull ByteBuffer body) {
        String[] contentType = request.getHeaders().get("content-type").split(";");
        byte[] boundary = null;
        for (String e : contentType) {
            String[] split = e.split("=");
            if (split.length == 2 && split[0].strip().toLowerCase().equals("boundary")) {
                boundary = ("--" + split[1].strip()).getBytes(StandardCharsets.UTF_8);
            }
        }

        if (boundary == null) {
            throw new ParseException("Invalid content-type header");
        }


        Map<String, BodyData.BodyField> ret = new HashMap<>();

        body.position(0);

        // Account for -- at beginning and end of final boundary line
        while (body.position() + boundary.length + 2 < body.limit()) {
            BodyData.BodyField field = parseChunk(body, boundary);
            if (field == null) {
                break;
            }
            ret.put(field.getFieldName(), field);
        }

        return new BodyData(ret);
    }
}
