package com.atomforge.fdo.binary;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.atomforge.fdo.binary.EncodingStyle.*;

/**
 * Encodes AtomFrame objects into binary format.
 * Can use compact encoding (automatic style selection) or full-only encoding.
 */
public final class BinaryEncoder {

    private final ByteArrayOutputStream output;
    private int currentProtocol = 0;
    private boolean useFullStyleOnly = true;  // Default to FULL style for binary format compatibility

    public BinaryEncoder() {
        this.output = new ByteArrayOutputStream(1024);
    }

    /**
     * Set whether to use FULL style only (for binary format compatibility) or compact encoding.
     */
    public BinaryEncoder setFullStyleOnly(boolean fullOnly) {
        this.useFullStyleOnly = fullOnly;
        return this;
    }

    /**
     * Encode a list of atom frames to binary.
     */
    public byte[] encode(List<AtomFrame> frames) throws FdoException {
        output.reset();
        currentProtocol = 0;

        for (AtomFrame frame : frames) {
            encodeFrame(frame);
        }

        return output.toByteArray();
    }

    /**
     * Encode a single atom frame.
     */
    public void encodeFrame(AtomFrame frame) throws FdoException {
        int protocol = frame.protocol();
        int atom = frame.atomNumber();
        byte[] data = frame.data();
        int dataLen = data.length;

        // Check if we need extended protocol encoding
        if (Protocol.isExtendedProtocol(protocol)) {
            encodeWithPrefix(frame);
            return;
        }

        // Use FULL style only for binary format compatibility
        if (useFullStyleOnly) {
            writeByte(FULL.encodeFirstByte(protocol));
            writeByte(atom);
            writeLength(dataLen);
            writeBytes(data);
            currentProtocol = protocol;
            return;
        }

        // Try to find the most compact encoding
        boolean sameProtocol = (protocol == currentProtocol);
        boolean atomFits = atom < 32;

        // STYLE_ATOM: No data, atom < 32, same protocol
        if (dataLen == 0 && atomFits && sameProtocol) {
            writeByte(ATOM.encodeFirstByte(atom));
            return;
        }

        // STYLE_ZERO: Data is [0], atom < 32, same protocol
        if (dataLen == 1 && data[0] == 0 && atomFits && sameProtocol) {
            writeByte(ZERO.encodeFirstByte(atom));
            return;
        }

        // STYLE_ONE: Data is [1], atom < 32, same protocol
        if (dataLen == 1 && data[0] == 1 && atomFits && sameProtocol) {
            writeByte(ONE.encodeFirstByte(atom));
            return;
        }

        // STYLE_DATA: Single byte data 0-7, atom < 32
        if (dataLen == 1 && atomFits) {
            int dataVal = data[0] & 0xFF;
            if (dataVal <= 7) {
                writeByte(DATA.encodeFirstByte(protocol));
                writeByte((dataVal << 5) | atom);
                currentProtocol = protocol;
                return;
            }
        }

        // STYLE_LENGTH: Data 1-7 bytes, atom < 32
        if (dataLen >= 1 && dataLen <= 7 && atomFits) {
            writeByte(LENGTH.encodeFirstByte(protocol));
            writeByte((dataLen << 5) | atom);
            writeBytes(data);
            currentProtocol = protocol;
            return;
        }

        // STYLE_CURRENT: Same protocol, any atom, any data
        if (sameProtocol && atomFits && dataLen <= MAX_SINGLE_BYTE_LENGTH) {
            writeByte(CURRENT.encodeFirstByte(atom));
            writeLength(dataLen);
            writeBytes(data);
            return;
        }

        // STYLE_FULL: General case
        writeByte(FULL.encodeFirstByte(protocol));
        writeByte(atom);
        writeLength(dataLen);
        writeBytes(data);
        currentProtocol = protocol;
    }

    /**
     * Encode with PREFIX style for extended protocols (32-127).
     *
     * PREFIX encoding format: [111PPAAS] [style|proto_low] [atom] [len] [data...]
     * Where:
     * - 111 = PREFIX style marker (bits 7-5)
     * - PP = protocol high bits 6-5 (bits 4-3)
     * - AA = atom high bits 7-6 (bits 2-1) - for LENGTH/DATA inner styles only
     * - S = style bit (bit 0)
     *
     * For FULL inner style, atom byte contains full 8-bit atom number.
     */
    private void encodeWithPrefix(AtomFrame frame) throws FdoException {
        int protocol = frame.protocol();
        int atom = frame.atomNumber();
        byte[] data = frame.data();
        int dataLen = data.length;

        // Extract protocol high bits
        int protocolHigh = (protocol >> 5) & 0x03;  // bits 6-5 of protocol
        int protocolLow = protocol & STYLE_MASK;    // Lower 5 bits

        // Build prefix byte: 111 PP AA S
        // Using FULL inner style, so AA=0 and S=0 (atom byte is full 8-bit atom number)
        int prefixByte = (PREFIX.getCode() << 5) | (protocolHigh << 3) | 0;

        writeByte(prefixByte);
        // Second byte: FULL style with protocolLow
        writeByte(FULL.encodeFirstByte(protocolLow));
        // Third byte: full atom number (for FULL inner style)
        writeByte(atom);
        writeLength(dataLen);
        writeBytes(data);

        currentProtocol = protocol;
    }

    /**
     * Write variable-length field.
     * If length <= 127, single byte with high bit clear.
     * If length > 127, two bytes: [0x80 | high] [low]
     */
    private void writeLength(int length) {
        if (length <= MAX_SINGLE_BYTE_LENGTH) {
            writeByte(length);
        } else {
            int highByte = 0x80 | (length >> 8);
            int lowByte = length & 0xFF;
            writeByte(highByte);
            writeByte(lowByte);
        }
    }

    private void writeByte(int b) {
        output.write(b);
    }

    private void writeBytes(byte[] bytes) {
        try {
            output.write(bytes);
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset the encoder state for a new encoding session.
     */
    public void reset() {
        output.reset();
        currentProtocol = 0;
    }

    /**
     * Get the current encoded size.
     */
    public int size() {
        return output.size();
    }
}
