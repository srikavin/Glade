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

import me.infuzion.web.server.util.Utilities;

import java.nio.ByteBuffer;
import java.util.Arrays;

class WebsocketFrameHeader {
    /**
     * 1 bit
     * Indicates if this frame is the final fragment of a message
     */
    boolean fin;
    /**
     * 1 bit
     * Reserved for extension use
     */
    boolean rsv1;
    /**
     * 1 bit
     * Reserved for extension use
     */
    boolean rsv2;
    /**
     * 1 bit
     * Reserved for extension use
     */
    boolean rsv3;
    /**
     * 4 bits
     */
    WebsocketFrameOpcodes opcode;
    /**
     * 1 bit
     * Whether the payload is masked
     */
    boolean mask;
    /**
     * 1 bit
     * 7, 7 + 16, or 7 + 64 bits
     * The length of the payload data
     */
    long payloadLength;
    /**
     * 0 or 4 bytes
     * Only present if {@link #mask} is true
     */
    byte[] maskingKey;

    /**
     * Attempts to parse all of the information in the given frame into this object. If an incomplete frame is provided,
     * the frame will be partially parsed, and false will be returned. If a full frame is available in the buffer,
     * true will be returned.
     * <p>
     * The buffer is assumed to contain the frame at position 0. Calls to this method will update the position of the
     * buffer to only parse bytes that were not previously parsed. Multiple calls to this method with the same buffer
     * will avoid duplicate parsing.
     * <p>
     * If the buffer is fully parsed, true will be returned, and the buffer's position will be set to the first byte
     * after the frame header.
     *
     * @param buffer     A buffer containing a partial or complete websocket frame header at position zero. If the position
     *                   of the buffer is not 0, those will have been assumed to have been already parsed. The position of
     *                   this buffer will be updated.
     * @param amountRead The total amount read into the buffer. For example, if the buffer contains uninitialized data
     *                   at the end, those would not be included in the amount read.
     * @return True if the frame header has been fully decoded, false otherwise.
     */
    public boolean parseFrom(ByteBuffer buffer, int amountRead) {
         /*
              0               1               2               3
              0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             +-+-+-+-+-------+-+-------------+-------------------------------+
             |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
             |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
             |N|V|V|V|       |S|             |   (if payload len==126/127)   |
             | |1|2|3|       |K|             |                               |
             +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
             |     Extended payload length continued, if payload len == 127  |
             + - - - - - - - - - - - - - - - +-------------------------------+
             |                               |Masking-key, if MASK set to 1  |
             +-------------------------------+-------------------------------+

          https://tools.ietf.org/html/rfc6455#section-5.2
        */
        if (amountRead < 1) {
            return false;
        }

        if (buffer.position() == 0) {
            byte b0 = buffer.get();

            this.fin = (b0 >> 7) != 0;
            this.rsv1 = ((b0 >> 6) & 1) != 0;
            this.rsv2 = ((b0 >> 5) & 1) != 0;
            this.rsv3 = ((b0 >> 4) & 1) != 0;
            this.opcode = WebsocketFrameOpcodes.fromByteValue((byte) (b0 & 0b1111));
        }

        if (amountRead < 2) {
            return false;
        }

        if (buffer.position() == 1) {
            byte b1 = buffer.get();

            this.mask = (b1 >> 7) != 0;
            this.payloadLength = ((b1 & (byte) 0b0111_1111)) & 0xFF;
        }

        long payloadLength = this.payloadLength;

        int payloadLengthEndPosition = 2;

        if (payloadLength == 126) {
            // Interpret the next 2 bytes as an unsigned 16-bit integer
            if (amountRead < 4) {
                // Wait for more data to arrive
                return false;
            }

            this.payloadLength = buffer.getShort(2) & 0xFF_FF;
            payloadLengthEndPosition = 4;
        } else if (payloadLength == 127) {
            // Interpret the next 8 bytes as an unsigned 64-bit integer
            if (amountRead < 10) {
                // Wait for more data to arrive
                return false;
            }

            this.payloadLength = buffer.getLong(2) & Long.MAX_VALUE;

            payloadLengthEndPosition = 10;
        }

        int headerEndPosition = payloadLengthEndPosition;

        if (amountRead >= payloadLengthEndPosition + 4 && this.mask) {
            this.maskingKey = new byte[]{
                    buffer.get(payloadLengthEndPosition),
                    buffer.get(payloadLengthEndPosition + 1),
                    buffer.get(payloadLengthEndPosition + 2),
                    buffer.get(payloadLengthEndPosition + 3)
            };
            headerEndPosition += 4;
        } else if (this.mask) {
            return false;
        }

        buffer.position(headerEndPosition);
        buffer.limit(amountRead);

        return true;
    }


    public ByteBuffer toByteBuffer(ByteBuffer payload) {
        byte b0 = 0;

        b0 += this.fin ? 1 << 7 : 0;
        b0 += this.rsv1 ? 1 << 6 : 0;
        b0 += this.rsv2 ? 1 << 5 : 0;
        b0 += this.rsv3 ? 1 << 4 : 0;
        b0 += this.opcode.value;

        byte b1 = 0;

        // ignore mask bit
        // b1 += header.mask ? 0 : 1 << 7;

        ByteBuffer buffer;

        int payloadSize = payload.limit();

        if (payloadSize <= 125) {
            buffer = ByteBuffer.allocate(2 + payloadSize);
            b1 += payloadSize;
        } else if (payloadSize < 65536) {
            buffer = ByteBuffer.allocate(2 + 2 + payloadSize);
            b1 += 126;
        } else {
            buffer = ByteBuffer.allocate(2 + 8 + payloadSize);
            b1 += 127;
        }

        buffer.put(b0);
        buffer.put(b1);

        if (payloadSize > 125 && payloadSize <= 65535) {
            buffer.putShort((short) payloadSize);
        } else if (payloadSize > 125) {
            buffer.putLong(payloadSize);
        }

        payload.rewind();

        buffer.put(payload);

        return buffer.rewind();
    }

    @Override
    public String toString() {
        return "WebsocketFrameHeader{" +
                "fin=" + fin +
                ", rsv1=" + rsv1 +
                ", rsv2=" + rsv2 +
                ", rsv3=" + rsv3 +
                ", opcode=" + opcode +
                ", mask=" + mask +
                ", payloadLength=" + payloadLength +
                ", maskingKey=" + Arrays.toString(Utilities.convertUnsignedByteArrayToIntArray(maskingKey)) +
                '}';
    }
}
