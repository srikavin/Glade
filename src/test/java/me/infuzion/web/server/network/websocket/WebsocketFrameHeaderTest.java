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

package me.infuzion.web.server.network.websocket;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WebsocketFrameHeaderTest {

    @Test
    void parseFromUnmasked() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{(byte) 0x81, 0x05, 0x48, 0x6c, 0x6c, 0x6f});

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, buffer.limit());

        assertTrue(result);

        assertEquals(buffer.position(), 2);
        assertEquals(buffer.limit(), 6);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);

        assertEquals(WebsocketFrameOpcodes.TEXT, header.opcode);
        assertFalse(header.mask);

        assertEquals(5, header.payloadLength);
        assertNull(header.maskingKey);
    }

    @Test
    void parseMasked() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{(byte) 0x81, (byte) 0x85, 0x37, (byte) 0xfa, 0x21, 0x3d, 0x7f, (byte) 0x9f, 0x4d, 0x51, 0x58});

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, buffer.limit());

        assertTrue(result);

        assertEquals(buffer.position(), 6);
        assertEquals(buffer.limit(), 11);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);

        assertEquals(WebsocketFrameOpcodes.TEXT, header.opcode);
        assertTrue(header.mask);

        assertEquals(5, header.payloadLength);
        assertArrayEquals(new byte[]{0x37, (byte) 0xfa, 0x21, 0x3d}, header.maskingKey);
    }

    @Test
    void parseUnmaskedPing() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{(byte) 0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f});

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, buffer.limit());

        assertTrue(result);

        assertEquals(buffer.position(), 2);
        assertEquals(buffer.limit(), 7);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);

        assertEquals(WebsocketFrameOpcodes.PING, header.opcode);
        assertFalse(header.mask);

        assertEquals(5, header.payloadLength);
        assertNull(header.maskingKey);
    }

    @Test
    void parseBinary256() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 256);
        buffer.put(new byte[]{(byte) 0x82, 0x7e, (byte) 0x01, 0x00});
        buffer.rewind();

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, buffer.limit());

        assertTrue(result);

        assertEquals(buffer.position(), 4);
        assertEquals(buffer.limit(), 4 + 256);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);

        assertEquals(WebsocketFrameOpcodes.BINARY, header.opcode);
        assertFalse(header.mask);

        assertEquals(256, header.payloadLength);
        assertNull(header.maskingKey);
    }

    @Test
    void parseBinary64kb() {
        ByteBuffer buffer = ByteBuffer.allocate(10 + 65536);
        buffer.put(new byte[]{(byte) 0x82, 0x7f});
        buffer.putLong(65536);
        buffer.rewind();

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, buffer.limit());

        assertTrue(result);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);

        assertEquals(WebsocketFrameOpcodes.BINARY, header.opcode);
        assertFalse(header.mask);

        assertEquals(65536, header.payloadLength);
        assertNull(header.maskingKey);
    }

    @Test
    void parseIncomplete() {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        boolean result = header.parseFrom(buffer, 0);
        assertFalse(result);

        // byte 1
        buffer.put(0, (byte) 0x81);

        result = header.parseFrom(buffer, 1);
        assertFalse(result);

        assertTrue(header.fin);
        assertFalse(header.rsv1);
        assertFalse(header.rsv2);
        assertFalse(header.rsv3);
        assertEquals(WebsocketFrameOpcodes.TEXT, header.opcode);


        // byte 2
        buffer.put(1, (byte) 0x85);

        result = header.parseFrom(buffer, 2);
        assertFalse(result);

        assertTrue(header.mask);
        assertEquals(5, header.payloadLength);


        // bytes 2-4
        buffer.put(2, (byte) 0x37);
        buffer.put(3, (byte) 0xfa);
        buffer.put(4, (byte) 0x21);

        result = header.parseFrom(buffer, 5);
        assertFalse(result);


        // byte 6
        buffer.put(5, (byte) 0x3d);

        result = header.parseFrom(buffer, 6);
        assertTrue(result);

        assertEquals(6, buffer.limit());
        assertEquals(6, buffer.position());
    }

    @Test
    void toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{(byte) 0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f});

        WebsocketFrameHeader header = new WebsocketFrameHeader();

        header.parseFrom(buffer, buffer.limit());

        ByteBuffer output = header.toByteBuffer(ByteBuffer.wrap(new byte[]{0x48, 0x65, 0x6c, 0x6c, 0x6f}));

        assertArrayEquals(new byte[]{(byte) 0x89, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f}, output.array());
    }

    @Test
    void toByteBuffer256() {
        //generate random payload
        byte[] payload = new byte[256];

        Random random = new Random(123L);
        random.nextBytes(payload);

        ByteBuffer buffer = ByteBuffer.allocate(4 + payload.length);
        buffer.put(new byte[]{(byte) 0x82, 0x7e, (byte) 0x01, 0x00});
        buffer.put(payload);
        buffer.rewind();

        WebsocketFrameHeader header = new WebsocketFrameHeader();
        header.parseFrom(buffer, buffer.limit());

        ByteBuffer output = header.toByteBuffer(ByteBuffer.wrap(payload));

        assertArrayEquals(buffer.array(), output.array());
    }

    @Test
    void toByteBuffer64kb() {
        //generate random payload
        byte[] payload = new byte[65536];

        Random random = new Random(123L);
        random.nextBytes(payload);

        ByteBuffer buffer = ByteBuffer.allocate(10 + payload.length);
        buffer.put(new byte[]{(byte) 0x82, 0x7f});
        buffer.putLong(65536);
        buffer.put(payload);
        buffer.rewind();

        WebsocketFrameHeader header = new WebsocketFrameHeader();
        header.parseFrom(buffer, buffer.limit());

        ByteBuffer output = header.toByteBuffer(ByteBuffer.wrap(payload));

        assertArrayEquals(buffer.array(), output.array());
    }
}