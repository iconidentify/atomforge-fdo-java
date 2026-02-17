package com.atomforge.fdo.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits large atoms that exceed the P3 frame size limit into continuation segments.
 *
 * <p>When an atom's encoded form exceeds the maximum frame payload size, it must
 * be split using the UNI continuation protocol:</p>
 *
 * <ul>
 *   <li><b>UNI_START_LARGE_ATOM (4)</b>: Contains protocol, atom#, total length header</li>
 *   <li><b>UNI_LARGE_ATOM_SEGMENT (5)</b>: Continuation segments with raw data</li>
 *   <li><b>UNI_END_LARGE_ATOM (6)</b>: Final segment with remaining data</li>
 * </ul>
 *
 * <p>Wire format per P3 spec:</p>
 * <pre>
 * UNI_START_LARGE_ATOM: [protocol] [atom#] [total_len (1-2 bytes)]
 * UNI_LARGE_ATOM_SEGMENT: [raw continuation bytes]
 * UNI_END_LARGE_ATOM: [final data bytes]
 * </pre>
 *
 * <p>Example: A 300-byte atom on protocol 27, atom 5 becomes:
 * <ol>
 *   <li>Frame 1: UNI_START_LARGE_ATOM with header (27, 5, 300)</li>
 *   <li>Frame 2: UNI_LARGE_ATOM_SEGMENT with ~115 bytes</li>
 *   <li>Frame 3: UNI_LARGE_ATOM_SEGMENT with ~115 bytes</li>
 *   <li>Frame 4: UNI_END_LARGE_ATOM with remaining ~70 bytes</li>
 * </ol>
 *
 * @see FrameAwareEncoder
 */
public final class LargeAtomSplitter {

    /** UNI protocol number */
    private static final int PROTO_UNIVERSE = 0;

    /** Atom number for UNI_START_LARGE_ATOM */
    public static final int UNI_START_LARGE_ATOM = 4;

    /** Atom number for UNI_LARGE_ATOM_SEGMENT */
    public static final int UNI_LARGE_ATOM_SEGMENT = 5;

    /** Atom number for UNI_END_LARGE_ATOM */
    public static final int UNI_END_LARGE_ATOM = 6;

    private final AtomFrame original;
    private final int maxDataPerSegment;

    /**
     * Creates a splitter for a large atom.
     *
     * @param original          The original atom frame to split
     * @param maxFrameSize      Maximum frame payload size (e.g., 119 for P3)
     */
    public LargeAtomSplitter(AtomFrame original, int maxFrameSize) {
        this.original = original;
        // Each continuation segment needs FULL style overhead:
        // 1 byte style|proto + 1 byte atom + 1-2 bytes length
        // For safety, assume 4 bytes overhead
        this.maxDataPerSegment = maxFrameSize - 4;
    }

    /**
     * Split the large atom into encoded continuation frames.
     *
     * <p>Returns a list of encoded atom segments, each ready to be written
     * to the output stream. The caller should flush frames between segments
     * as needed.</p>
     *
     * @return List of encoded atom bytes (START, SEGMENT(s), END)
     */
    public List<AtomFrame> split() {
        List<AtomFrame> segments = new ArrayList<>();
        byte[] data = original.data();
        int totalLength = data.length;

        // 1. Create START_LARGE_ATOM header
        byte[] startData = encodeStartHeader(original.protocol(), original.atomNumber(), totalLength);
        segments.add(new AtomFrame(PROTO_UNIVERSE, UNI_START_LARGE_ATOM, startData));

        // 2. Calculate data that fits in first frame after START atom
        // The START atom is its own frame, so data begins in next frame
        int offset = 0;

        // 3. Create SEGMENT atoms for middle chunks
        while (offset + maxDataPerSegment < totalLength) {
            byte[] segmentData = new byte[maxDataPerSegment];
            System.arraycopy(data, offset, segmentData, 0, maxDataPerSegment);
            segments.add(new AtomFrame(PROTO_UNIVERSE, UNI_LARGE_ATOM_SEGMENT, segmentData));
            offset += maxDataPerSegment;
        }

        // 4. Create END_LARGE_ATOM with remaining data
        int remaining = totalLength - offset;
        byte[] endData = new byte[remaining];
        if (remaining > 0) {
            System.arraycopy(data, offset, endData, 0, remaining);
        }
        segments.add(new AtomFrame(PROTO_UNIVERSE, UNI_END_LARGE_ATOM, endData));

        return segments;
    }

    /**
     * Encode the UNI_START_LARGE_ATOM header.
     *
     * <p>Format: [protocol] [atom#] [length (1-2 bytes)]</p>
     *
     * @param protocol    Original atom's protocol
     * @param atomNumber  Original atom's number
     * @param totalLength Total data length
     * @return Encoded header bytes
     */
    private byte[] encodeStartHeader(int protocol, int atomNumber, int totalLength) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(protocol);
        out.write(atomNumber);

        // Encode length as plain 16-bit big-endian (NOT high-bit variable encoding)
        // The client expects raw 16-bit length, not FDO variable-length format
        if (totalLength <= 127) {
            out.write(totalLength);
        } else {
            out.write((totalLength >> 8) & 0xFF);  // High byte (plain)
            out.write(totalLength & 0xFF);          // Low byte
        }

        return out.toByteArray();
    }

    /**
     * Check if an atom needs to be split based on frame size.
     *
     * @param frame        The atom frame to check
     * @param maxFrameSize Maximum frame payload size
     * @return true if the atom's encoded size exceeds the frame limit
     */
    public static boolean needsSplitting(AtomFrame frame, int maxFrameSize) {
        // Calculate encoded size using FULL style:
        // 1 byte style|proto + 1 byte atom + length bytes + data
        int encodedSize = calculateEncodedSize(frame);
        return encodedSize > maxFrameSize;
    }

    /**
     * Calculate the encoded size of an atom frame using FULL style.
     *
     * @param frame The atom frame
     * @return Encoded size in bytes
     */
    public static int calculateEncodedSize(AtomFrame frame) {
        int dataLen = frame.data().length;
        // FULL style: [style|proto] [atom] [length] [data]
        // Length encoding: 1 byte if <= 127, 2 bytes otherwise
        int lengthBytes = (dataLen <= 127) ? 1 : 2;
        return 1 + 1 + lengthBytes + dataLen;
    }
}
