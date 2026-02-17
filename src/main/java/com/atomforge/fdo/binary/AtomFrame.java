package com.atomforge.fdo.binary;

import java.util.Arrays;

/**
 * Represents a decoded atom from the binary stream.
 * Contains the protocol, atom number, and raw data bytes.
 */
public record AtomFrame(
    int protocol,
    int atomNumber,
    byte[] data,
    EncodingStyle style
) {
    /**
     * Create an AtomFrame, making a defensive copy of the data.
     */
    public AtomFrame {
        if (protocol < 0 || protocol > 127) {
            throw new IllegalArgumentException("protocol must be 0-127, got: " + protocol);
        }
        if (atomNumber < 0 || atomNumber > 255) {
            throw new IllegalArgumentException("atomNumber must be 0-255, got: " + atomNumber);
        }
        data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
    }

    /**
     * Create an AtomFrame without style information.
     */
    public AtomFrame(int protocol, int atomNumber, byte[] data) {
        this(protocol, atomNumber, data, null);
    }

    /**
     * Create an AtomFrame with no data.
     */
    public static AtomFrame noData(int protocol, int atomNumber) {
        return new AtomFrame(protocol, atomNumber, new byte[0], null);
    }

    /**
     * Create an AtomFrame with single byte data.
     */
    public static AtomFrame singleByte(int protocol, int atomNumber, int value) {
        return new AtomFrame(protocol, atomNumber, new byte[]{(byte) value}, null);
    }

    /**
     * Get the data length.
     */
    public int dataLength() {
        return data.length;
    }

    /**
     * Check if this frame has data.
     */
    public boolean hasData() {
        return data.length > 0;
    }

    /**
     * Get data as a copy (defensive).
     */
    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Get unique key for this atom (protocol << 16 | atom).
     */
    public long key() {
        return ((long) protocol << 16) | atomNumber;
    }

    /**
     * Get data as unsigned byte at index.
     */
    public int getUnsignedByte(int index) {
        if (index < 0 || index >= data.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + data.length);
        }
        return data[index] & 0xFF;
    }

    /**
     * Get data as big-endian 32-bit integer.
     */
    public int getInt32BE() {
        if (data.length < 4) {
            // Pad with zeros on the left
            int result = 0;
            for (byte b : data) {
                result = (result << 8) | (b & 0xFF);
            }
            return result;
        }
        return ((data[0] & 0xFF) << 24) |
               ((data[1] & 0xFF) << 16) |
               ((data[2] & 0xFF) << 8) |
               (data[3] & 0xFF);
    }

    /**
     * Get data as null-terminated string.
     */
    public String getString() {
        int len = data.length;
        // Find null terminator
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                len = i;
                break;
            }
        }
        return new String(data, 0, len, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    @Override
    public String toString() {
        return String.format("AtomFrame[proto=%d, atom=%d, len=%d, style=%s]",
            protocol, atomNumber, data.length, style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtomFrame other)) return false;
        return protocol == other.protocol &&
               atomNumber == other.atomNumber &&
               Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = protocol;
        result = 31 * result + atomNumber;
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
