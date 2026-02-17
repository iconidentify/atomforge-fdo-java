package com.atomforge.fdo.binary;

import com.atomforge.fdo.FdoException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.atomforge.fdo.binary.EncodingStyle.*;

/**
 * Decodes binary atom streams into AtomFrame objects.
 * Supports all 8 encoding styles and large atom reassembly.
 */
public final class BinaryDecoder {

    // Large atom continuation atom numbers (UNI protocol = 0)
    private static final int UNI_START_LARGE_ATOM = 4;
    private static final int UNI_LARGE_ATOM_SEGMENT = 5;
    private static final int UNI_END_LARGE_ATOM = 6;

    private final ByteBuffer buffer;
    private int currentProtocol = 0;

    // Large atom reassembly state
    private int largeAtomProto = -1;
    private int largeAtomNum = -1;
    private ByteArrayOutputStream largeAtomBuffer = null;

    public BinaryDecoder(byte[] data) {
        this.buffer = ByteBuffer.wrap(data);
        this.buffer.order(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Decode all atoms from the binary data.
     * Handles large atom reassembly (UNI atoms 4, 5, 6).
     */
    public List<AtomFrame> decode() throws FdoException {
        List<AtomFrame> frames = new ArrayList<>();
        while (buffer.hasRemaining()) {
            AtomFrame frame = decodeNext();
            AtomFrame result = handleLargeAtom(frame);
            if (result != null) {
                frames.add(result);
            }
            // If result is null, it was a large atom control frame that was absorbed
        }
        return frames;
    }

    /**
     * Handle large atom continuation sequences.
     * Returns the frame to emit, or null if frame was absorbed into reassembly buffer.
     */
    private AtomFrame handleLargeAtom(AtomFrame frame) throws FdoException {
        // Only UNI protocol (0) atoms 4, 5, 6 are large atom control
        if (frame.protocol() != 0) {
            return frame;
        }

        switch (frame.atomNumber()) {
            case UNI_START_LARGE_ATOM:
                // Data: [target_protocol, target_atom, initial_data...]
                byte[] startData = frame.data();
                if (startData.length < 2) {
                    throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                        "UNI_START_LARGE_ATOM requires at least 2 bytes (proto, atom)");
                }
                largeAtomProto = startData[0] & 0xFF;
                largeAtomNum = startData[1] & 0xFF;
                largeAtomBuffer = new ByteArrayOutputStream();
                // Append initial data after proto/atom bytes
                if (startData.length > 2) {
                    largeAtomBuffer.write(startData, 2, startData.length - 2);
                }
                return null; // Absorb this control frame

            case UNI_LARGE_ATOM_SEGMENT:
                // Data: [continuation_data...]
                if (largeAtomBuffer == null) {
                    // No active large atom - pass through (shouldn't happen in valid data)
                    return frame;
                }
                try {
                    largeAtomBuffer.write(frame.data());
                } catch (Exception e) {
                    throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                        "Failed to append large atom segment: " + e.getMessage());
                }
                return null; // Absorb this control frame

            case UNI_END_LARGE_ATOM:
                // Data: [final_data...]
                if (largeAtomBuffer == null) {
                    // No active large atom - pass through (shouldn't happen in valid data)
                    return frame;
                }
                try {
                    largeAtomBuffer.write(frame.data());
                } catch (Exception e) {
                    throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                        "Failed to append large atom final segment: " + e.getMessage());
                }
                // Emit the complete reassembled atom
                AtomFrame completeAtom = new AtomFrame(
                    largeAtomProto,
                    largeAtomNum,
                    largeAtomBuffer.toByteArray(),
                    FULL // Mark as FULL style since it's reassembled
                );
                // Clear state
                largeAtomProto = -1;
                largeAtomNum = -1;
                largeAtomBuffer = null;
                return completeAtom;

            default:
                // Not a large atom control frame
                return frame;
        }
    }

    /**
     * Decode the next atom from the buffer.
     */
    public AtomFrame decodeNext() throws FdoException {
        if (!buffer.hasRemaining()) {
            throw new FdoException(FdoException.ErrorCode.UNEXPECTED_EOF, "Unexpected end of binary data");
        }

        int firstByte = buffer.get() & 0xFF;
        EncodingStyle style = EncodingStyle.fromByte(firstByte);

        return switch (style) {
            case FULL -> decodeStyleFull(firstByte);
            case LENGTH -> decodeStyleLength(firstByte);
            case DATA -> decodeStyleData(firstByte);
            case ATOM -> decodeStyleAtom(firstByte);
            case CURRENT -> decodeStyleCurrent(firstByte);
            case ZERO -> decodeStyleZero(firstByte);
            case ONE -> decodeStyleOne(firstByte);
            case PREFIX -> decodeStylePrefix(firstByte);
        };
    }

    /**
     * STYLE_FULL: [style|proto] [atom] [len] [data...]
     */
    private AtomFrame decodeStyleFull(int firstByte) throws FdoException {
        int protocol = firstByte & STYLE_MASK;
        currentProtocol = protocol;

        int atomNumber = readByte();
        int length = readLength();
        byte[] data = readBytes(length);

        return new AtomFrame(protocol, atomNumber, data, FULL);
    }

    /**
     * STYLE_LENGTH: [style|proto] [len|atom] [data...]
     * Length is 1-7 bytes (high 3 bits), atom < 32 (low 5 bits)
     */
    private AtomFrame decodeStyleLength(int firstByte) throws FdoException {
        int protocol = firstByte & STYLE_MASK;
        currentProtocol = protocol;

        int secondByte = readByte();
        int length = (secondByte >> 5) & 0x07;
        int atomNumber = secondByte & STYLE_MASK;

        byte[] data = readBytes(length);
        return new AtomFrame(protocol, atomNumber, data, LENGTH);
    }

    /**
     * STYLE_DATA: [style|proto] [data|atom]
     * Data is single byte 0-7 (high 3 bits), atom < 32 (low 5 bits)
     */
    private AtomFrame decodeStyleData(int firstByte) throws FdoException {
        int protocol = firstByte & STYLE_MASK;
        currentProtocol = protocol;

        int secondByte = readByte();
        int dataValue = (secondByte >> 5) & 0x07;
        int atomNumber = secondByte & STYLE_MASK;

        byte[] data = new byte[]{(byte) dataValue};
        return new AtomFrame(protocol, atomNumber, data, DATA);
    }

    /**
     * STYLE_ATOM: [style|atom]
     * No data, atom < 32
     */
    private AtomFrame decodeStyleAtom(int firstByte) {
        int atomNumber = firstByte & STYLE_MASK;
        // Use current protocol context
        return new AtomFrame(currentProtocol, atomNumber, new byte[0], ATOM);
    }

    /**
     * STYLE_CURRENT: [style|atom] [len] [data...]
     * Uses current protocol context
     */
    private AtomFrame decodeStyleCurrent(int firstByte) throws FdoException {
        int atomNumber = firstByte & STYLE_MASK;
        int length = readLength();
        byte[] data = readBytes(length);

        return new AtomFrame(currentProtocol, atomNumber, data, CURRENT);
    }

    /**
     * STYLE_ZERO: [style|atom]
     * Implicit data = 0, atom < 32
     */
    private AtomFrame decodeStyleZero(int firstByte) {
        int atomNumber = firstByte & STYLE_MASK;
        byte[] data = new byte[]{0};
        return new AtomFrame(currentProtocol, atomNumber, data, ZERO);
    }

    /**
     * STYLE_ONE: [style|atom]
     * Implicit data = 1, atom < 32
     */
    private AtomFrame decodeStyleOne(int firstByte) {
        int atomNumber = firstByte & STYLE_MASK;
        byte[] data = new byte[]{1};
        return new AtomFrame(currentProtocol, atomNumber, data, ONE);
    }

    /**
     * STYLE_PREFIX: [111PPAAS] [style|proto] [atom] ...
     * For extended protocols 32-127
     * Prefix byte encodes: PP = proto bits 6-5, AA = atom bits 7-6, S = style bit
     */
    private AtomFrame decodeStylePrefix(int firstByte) throws FdoException {
        // Extract prefix components
        int protocolHigh = (firstByte >> 3) & 0x03;  // PP bits
        int atomHigh = (firstByte >> 1) & 0x03;       // AA bits
        int styleBit = firstByte & 0x01;              // S bit

        // Read second byte: [style|proto_low]
        int secondByte = readByte();
        int innerStyle = (secondByte >> 5) & 0x07;
        int protocolLow = secondByte & STYLE_MASK;

        // Combine protocol: high bits from prefix, low bits from second byte
        int protocol = (protocolHigh << 5) | protocolLow;
        currentProtocol = protocol;

        // Read atom byte
        int atomByte = readByte();
        int atomNumber;

        // Decode based on inner style
        EncodingStyle decodedStyle = EncodingStyle.fromByte(innerStyle << 5);
        int length;
        byte[] data;

        if (innerStyle == FULL.getCode() || innerStyle == CURRENT.getCode()) {
            // FULL/CURRENT: atom byte is full 8-bit atom number
            atomNumber = atomByte;
            length = readLength();
            data = readBytes(length);
        } else if (innerStyle == LENGTH.getCode()) {
            // Length encoded in high bits of atom byte
            length = (atomByte >> 5) & 0x07;
            atomNumber = (atomHigh << 6) | (atomByte & STYLE_MASK);
            data = readBytes(length);
        } else if (innerStyle == DATA.getCode()) {
            int dataValue = (atomByte >> 5) & 0x07;
            atomNumber = (atomHigh << 6) | (atomByte & STYLE_MASK);
            data = new byte[]{(byte) dataValue};
        } else if (innerStyle == ATOM.getCode()) {
            atomNumber = (atomHigh << 6) | (atomByte & 0x3F);
            data = new byte[0];
        } else if (innerStyle == ZERO.getCode()) {
            atomNumber = (atomHigh << 6) | (atomByte & 0x3F);
            data = new byte[]{0};
        } else if (innerStyle == ONE.getCode()) {
            atomNumber = (atomHigh << 6) | (atomByte & 0x3F);
            data = new byte[]{1};
        } else {
            atomNumber = atomByte;
            data = new byte[0];
        }

        return new AtomFrame(protocol, atomNumber, data, PREFIX);
    }

    /**
     * Read variable-length field.
     * If high bit clear (< 0x80), single byte length.
     * If high bit set (>= 0x80), two bytes: [0x80 | high] [low]
     */
    private int readLength() throws FdoException {
        int firstByte = readByte();
        if ((firstByte & 0x80) == 0) {
            // High bit clear = single byte length (0-127)
            return firstByte;
        }
        // High bit set = extended length
        int highByte = firstByte & 0x7F;
        int lowByte = readByte();
        return (highByte << 8) | lowByte;
    }

    private int readByte() throws FdoException {
        if (!buffer.hasRemaining()) {
            throw new FdoException(FdoException.ErrorCode.UNEXPECTED_EOF, "Unexpected end of binary data");
        }
        return buffer.get() & 0xFF;
    }

    private byte[] readBytes(int count) throws FdoException {
        if (buffer.remaining() < count) {
            throw new FdoException(FdoException.ErrorCode.UNEXPECTED_EOF,
                "Not enough data: need " + count + " bytes, have " + buffer.remaining());
        }
        byte[] data = new byte[count];
        buffer.get(data);
        return data;
    }

    /**
     * Get current position in the buffer.
     */
    public int position() {
        return buffer.position();
    }

    /**
     * Get remaining bytes in the buffer.
     */
    public int remaining() {
        return buffer.remaining();
    }

    /**
     * Check if there are more bytes to decode.
     */
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }
}
