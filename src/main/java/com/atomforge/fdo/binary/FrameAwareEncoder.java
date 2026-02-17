package com.atomforge.fdo.binary;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.FrameConsumer;
import com.atomforge.fdo.atom.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.atomforge.fdo.binary.EncodingStyle.*;

/**
 * Encodes atom frames with awareness of frame size boundaries.
 *
 * <p>Unlike {@link BinaryEncoder} which produces a single byte array,
 * this encoder produces output in frame-sized chunks suitable for
 * P3 or other size-limited transport protocols.</p>
 *
 * <p>Key guarantees:
 * <ul>
 *   <li>Atoms are never split across frame boundaries</li>
 *   <li>Large atoms use UNI continuation protocol (atoms 4, 5, 6)</li>
 *   <li>Frame size is configurable</li>
 *   <li>Protocol state is maintained across frames</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * FrameAwareEncoder encoder = new FrameAwareEncoder(119, consumer);
 * encoder.encode(atomFrames);
 * }</pre>
 *
 * @see FrameConsumer
 * @see LargeAtomSplitter
 */
public final class FrameAwareEncoder {

    private final int maxFrameSize;
    private final FrameConsumer consumer;
    private final ByteArrayOutputStream currentFrame;

    private int currentProtocol = 0;
    private int frameIndex = 0;

    /**
     * Creates a frame-aware encoder.
     *
     * @param maxFrameSize Maximum payload size per frame (e.g., 119 for P3)
     * @param consumer     Callback to receive completed frames
     */
    public FrameAwareEncoder(int maxFrameSize, FrameConsumer consumer) {
        if (maxFrameSize < 4) {
            throw new IllegalArgumentException("Frame size must be at least 4 bytes");
        }
        this.maxFrameSize = maxFrameSize;
        this.consumer = consumer;
        this.currentFrame = new ByteArrayOutputStream(maxFrameSize);
    }

    /**
     * Encode a list of atom frames, delivering output via the consumer callback.
     *
     * @param frames The atom frames to encode
     * @throws FdoException if encoding fails
     */
    public void encode(List<AtomFrame> frames) throws FdoException {
        currentFrame.reset();
        currentProtocol = 0;
        frameIndex = 0;

        for (AtomFrame frame : frames) {
            encodeFrame(frame);
        }

        // Flush any remaining data
        if (currentFrame.size() > 0) {
            flushCurrentFrame(true);
        } else if (frameIndex == 0) {
            // Empty input - still call consumer with empty final frame
            consumer.onFrame(new byte[0], 0, true);
        }
    }

    /**
     * Encode a single atom frame with frame boundary awareness.
     */
    private void encodeFrame(AtomFrame frame) throws FdoException {
        int encodedSize = calculateEncodedSize(frame);

        if (encodedSize > maxFrameSize) {
            // Large atom - needs splitting
            encodeLargeAtom(frame);
        } else if (currentFrame.size() + encodedSize > maxFrameSize) {
            // Would overflow - flush current frame first
            flushCurrentFrame(false);
            encodeAtomToCurrentFrame(frame);
        } else {
            // Fits in current frame
            encodeAtomToCurrentFrame(frame);
        }
    }

    /**
     * Encode a large atom using UNI continuation protocol.
     */
    private void encodeLargeAtom(AtomFrame frame) throws FdoException {
        LargeAtomSplitter splitter = new LargeAtomSplitter(frame, maxFrameSize);
        List<AtomFrame> segments = splitter.split();

        for (int i = 0; i < segments.size(); i++) {
            AtomFrame segment = segments.get(i);
            int segmentSize = calculateEncodedSize(segment);

            // Each segment should start a new frame (per P3 spec)
            if (currentFrame.size() > 0) {
                flushCurrentFrame(false);
            }

            encodeAtomToCurrentFrame(segment);

            // Flush after each segment except the last
            // (the last might fit with subsequent atoms)
            if (i < segments.size() - 1) {
                flushCurrentFrame(false);
            }
        }
    }

    /**
     * Encode an atom to the current frame buffer using FULL style.
     */
    private void encodeAtomToCurrentFrame(AtomFrame frame) throws FdoException {
        int protocol = frame.protocol();
        int atom = frame.atomNumber();
        byte[] data = frame.data();
        int dataLen = data.length;

        // Check if we need extended protocol encoding
        if (Protocol.isExtendedProtocol(protocol)) {
            encodeWithPrefix(frame);
            return;
        }

        // Use FULL style for consistency and binary format compatibility
        writeByte(FULL.encodeFirstByte(protocol));
        writeByte(atom);
        writeLength(dataLen);
        writeBytes(data);
        currentProtocol = protocol;
    }

    /**
     * Encode with PREFIX style for extended protocols (32-127).
     */
    private void encodeWithPrefix(AtomFrame frame) throws FdoException {
        int protocol = frame.protocol();
        int atom = frame.atomNumber();
        byte[] data = frame.data();
        int dataLen = data.length;

        // PREFIX encoding from reference guide:
        // prefix_byte = (STYLE_PREFIX << 5) | ((protocol & 0x60) >> 2)
        // Then normal encoding with protocol = protocol & 0x1F

        int prefixByte = (PREFIX.getCode() << 5) | ((protocol & 0x60) >> 2);
        int protocolLow = protocol & STYLE_MASK;  // Lower 5 bits

        writeByte(prefixByte);
        // Continue with FULL style using protocolLow
        writeByte(FULL.encodeFirstByte(protocolLow));
        writeByte(atom);
        writeLength(dataLen);
        writeBytes(data);

        currentProtocol = protocol;
    }

    /**
     * Flush the current frame buffer to the consumer.
     */
    private void flushCurrentFrame(boolean isLast) {
        if (currentFrame.size() > 0 || isLast) {
            consumer.onFrame(currentFrame.toByteArray(), frameIndex++, isLast);
            currentFrame.reset();
        }
    }

    /**
     * Calculate the encoded size of an atom frame using FULL style.
     */
    private int calculateEncodedSize(AtomFrame frame) {
        int dataLen = frame.data().length;
        int lengthBytes = (dataLen <= MAX_SINGLE_BYTE_LENGTH) ? 1 : 2;

        if (Protocol.isExtendedProtocol(frame.protocol())) {
            // PREFIX style: prefix + style|proto + atom + length + data
            return 1 + 1 + 1 + lengthBytes + dataLen;
        } else {
            // FULL style: style|proto + atom + length + data
            return 1 + 1 + lengthBytes + dataLen;
        }
    }

    /**
     * Write variable-length field.
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
        currentFrame.write(b);
    }

    private void writeBytes(byte[] bytes) {
        try {
            currentFrame.write(bytes);
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the number of frames produced so far.
     */
    public int getFrameCount() {
        return frameIndex;
    }

    /**
     * Get the current frame buffer size.
     */
    public int getCurrentFrameSize() {
        return currentFrame.size();
    }
}
