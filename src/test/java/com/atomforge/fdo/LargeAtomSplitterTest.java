package com.atomforge.fdo;

import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.LargeAtomSplitter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LargeAtomSplitter.
 */
class LargeAtomSplitterTest {

    @Test
    void needsSplitting_smallAtom_returnsFalse() {
        // Small atom: 10 bytes of data + 4 bytes overhead = 14 bytes
        byte[] data = new byte[10];
        AtomFrame frame = new AtomFrame(1, 5, data);

        assertThat(LargeAtomSplitter.needsSplitting(frame, 119))
            .as("10-byte atom should not need splitting")
            .isFalse();
    }

    @Test
    void needsSplitting_largeAtom_returnsTrue() {
        // Large atom: 200 bytes of data exceeds 119 byte frame
        byte[] data = new byte[200];
        AtomFrame frame = new AtomFrame(1, 5, data);

        assertThat(LargeAtomSplitter.needsSplitting(frame, 119))
            .as("200-byte atom should need splitting")
            .isTrue();
    }

    @Test
    void needsSplitting_exactlyAtLimit_returnsFalse() {
        // Exact fit: 119 - 4 (overhead) = 115 bytes data
        byte[] data = new byte[115];
        AtomFrame frame = new AtomFrame(1, 5, data);

        assertThat(LargeAtomSplitter.needsSplitting(frame, 119))
            .as("Atom exactly at limit should not need splitting")
            .isFalse();
    }

    @Test
    void calculateEncodedSize_smallData() {
        // 10 bytes data: 1 (style|proto) + 1 (atom) + 1 (length) + 10 (data) = 13
        byte[] data = new byte[10];
        AtomFrame frame = new AtomFrame(1, 5, data);

        assertThat(LargeAtomSplitter.calculateEncodedSize(frame))
            .as("Encoded size should include FULL style overhead")
            .isEqualTo(13);
    }

    @Test
    void calculateEncodedSize_largeData() {
        // 200 bytes data: 1 (style|proto) + 1 (atom) + 2 (length, >127) + 200 (data) = 204
        byte[] data = new byte[200];
        AtomFrame frame = new AtomFrame(1, 5, data);

        assertThat(LargeAtomSplitter.calculateEncodedSize(frame))
            .as("Encoded size should use 2-byte length for >127 bytes")
            .isEqualTo(204);
    }

    @Test
    void split_createsStartSegmentEnd() {
        // 300 bytes on protocol 27, atom 5
        byte[] data = new byte[300];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        AtomFrame original = new AtomFrame(27, 5, data);

        LargeAtomSplitter splitter = new LargeAtomSplitter(original, 119);
        List<AtomFrame> segments = splitter.split();

        assertThat(segments).hasSizeGreaterThanOrEqualTo(3)
            .as("Should have at least START, one SEGMENT, and END");

        // First segment should be UNI_START_LARGE_ATOM (protocol 0, atom 4)
        AtomFrame start = segments.get(0);
        assertThat(start.protocol()).isEqualTo(0);
        assertThat(start.atomNumber()).isEqualTo(LargeAtomSplitter.UNI_START_LARGE_ATOM);

        // Verify START header contains protocol, atom, and length
        byte[] startData = start.data();
        assertThat(startData[0]).isEqualTo((byte) 27); // original protocol
        assertThat(startData[1]).isEqualTo((byte) 5);  // original atom

        // Last segment should be UNI_END_LARGE_ATOM (protocol 0, atom 6)
        AtomFrame end = segments.get(segments.size() - 1);
        assertThat(end.protocol()).isEqualTo(0);
        assertThat(end.atomNumber()).isEqualTo(LargeAtomSplitter.UNI_END_LARGE_ATOM);

        // Middle segments should be UNI_LARGE_ATOM_SEGMENT (protocol 0, atom 5)
        for (int i = 1; i < segments.size() - 1; i++) {
            AtomFrame middle = segments.get(i);
            assertThat(middle.protocol()).isEqualTo(0);
            assertThat(middle.atomNumber()).isEqualTo(LargeAtomSplitter.UNI_LARGE_ATOM_SEGMENT);
        }
    }

    @Test
    void split_preservesAllData() {
        // Create atom with recognizable pattern
        byte[] original = new byte[500];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i % 256);
        }
        AtomFrame frame = new AtomFrame(10, 20, original);

        LargeAtomSplitter splitter = new LargeAtomSplitter(frame, 119);
        List<AtomFrame> segments = splitter.split();

        // Collect all data from segments (skip header in START)
        int totalRecovered = 0;
        byte[] recovered = new byte[original.length];

        // START segment header: [protocol] [atom] [length] - skip these
        AtomFrame start = segments.get(0);
        byte[] startData = start.data();
        // Length encoding: 1 byte if <= 127, 2 bytes otherwise
        int headerLen = (original.length <= 127) ? 3 : 4;

        // SEGMENT and END segments contain raw data
        for (int i = 1; i < segments.size(); i++) {
            byte[] segData = segments.get(i).data();
            System.arraycopy(segData, 0, recovered, totalRecovered, segData.length);
            totalRecovered += segData.length;
        }

        assertThat(totalRecovered).isEqualTo(original.length);
        assertThat(recovered).isEqualTo(original);
    }

    @Test
    void split_lengthEncodingSmall() {
        // 50 bytes - single byte length encoding
        byte[] data = new byte[50];
        AtomFrame original = new AtomFrame(1, 1, data);

        LargeAtomSplitter splitter = new LargeAtomSplitter(original, 10); // Force split with small frame
        List<AtomFrame> segments = splitter.split();

        // Check START header length encoding
        byte[] startData = segments.get(0).data();
        // [protocol=1] [atom=1] [length=50]
        assertThat(startData).hasSize(3);
        assertThat(startData[2]).isEqualTo((byte) 50);
    }

    @Test
    void split_lengthEncodingLarge() {
        // 300 bytes - two byte length encoding (plain 16-bit big-endian)
        byte[] data = new byte[300];
        AtomFrame original = new AtomFrame(1, 1, data);

        LargeAtomSplitter splitter = new LargeAtomSplitter(original, 50);
        List<AtomFrame> segments = splitter.split();

        // Check START header length encoding
        byte[] startData = segments.get(0).data();
        // [protocol=1] [atom=1] [high_byte] [low_byte]
        assertThat(startData).hasSize(4);
        // 300 = 0x012C, encoded as plain big-endian [0x01, 0x2C]
        assertThat(startData[2] & 0xFF).isEqualTo(0x01); // High byte (plain)
        assertThat(startData[3] & 0xFF).isEqualTo(0x2C); // Low byte (44)
    }

    @Test
    void split_emptyData() {
        // Edge case: empty atom data
        byte[] data = new byte[0];
        AtomFrame original = new AtomFrame(1, 1, data);

        // This shouldn't need splitting but let's test the split anyway
        LargeAtomSplitter splitter = new LargeAtomSplitter(original, 50);
        List<AtomFrame> segments = splitter.split();

        // Should have START and END
        assertThat(segments).hasSize(2);

        // END should have empty data
        assertThat(segments.get(1).data()).isEmpty();
    }

    @Test
    void split_varyingFrameSizes() {
        // Test with different max frame sizes
        byte[] data = new byte[200];
        AtomFrame original = new AtomFrame(5, 10, data);

        // Small frames = more segments
        LargeAtomSplitter small = new LargeAtomSplitter(original, 30);
        List<AtomFrame> smallSegments = small.split();

        // Large frames = fewer segments
        LargeAtomSplitter large = new LargeAtomSplitter(original, 100);
        List<AtomFrame> largeSegments = large.split();

        assertThat(smallSegments.size())
            .as("Smaller frames should produce more segments")
            .isGreaterThan(largeSegments.size());
    }
}
