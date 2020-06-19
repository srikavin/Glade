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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public class BodyData {
    private final @NotNull Map<String, BodyField> fields;

    public BodyData(@NotNull Map<String, BodyField> fields) {
        this.fields = fields;
    }

    public @NotNull Map<String, BodyField> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "BodyData{" +
                "fields=" + fields +
                '}';
    }

    public static class BodyField {
        @NotNull
        private final String fieldName;
        @Nullable
        private final String fileName;
        @NotNull
        private final ByteBuffer rawContent;
        @NotNull
        private final String content;
        @NotNull
        private final String contentType;
        @NotNull
        private final Charset encoding;

        public BodyField(@NotNull String fieldName, @Nullable String fileName, @NotNull byte[] rawContent,
                         @NotNull String contentType, @NotNull Charset encoding) {
            this(fieldName, fileName, ByteBuffer.wrap(rawContent), contentType, encoding);
        }

        public BodyField(@NotNull String fieldName, @Nullable String fileName, @NotNull ByteBuffer rawContent,
                         @NotNull String contentType, @NotNull Charset encoding) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.rawContent = rawContent;
            this.contentType = contentType;
            this.encoding = encoding;
            this.content = encoding.decode(rawContent).toString();
        }


        /**
         * @return The field name containing this data.
         */
        public @NotNull String getFieldName() {
            return fieldName;
        }

        /**
         * @return The file name associated with this field. May be null if no file name was specified.
         */
        public @Nullable String getFileName() {
            return fileName;
        }

        /**
         * @return The raw contents of this field. Any encoding is removed.
         */
        public @NotNull ByteBuffer getRawContent() {
            return rawContent.rewind();
        }

        /**
         * @return The content type of this field.
         */
        public @NotNull String getContentType() {
            return contentType;
        }

        /**
         * @return The specified encoding used to encode this field as specified by the request.
         */
        public @NotNull Charset getEncoding() {
            return encoding;
        }

        /**
         * @return The content as converted from {@link #getRawContent()} using the specified character encoding.
         */
        public @NotNull String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "BodyField{" +
                    "fieldName='" + fieldName + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", size='" + rawContent.limit() + '\'' +
                    ", encoding=" + encoding +
                    '}';
        }
    }
}
