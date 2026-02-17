package com.atomforge.fdo;

import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.FrameAwareEncoder;
import com.atomforge.fdo.binary.LargeAtomSplitter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FrameAwareEncoder.
 */
class FrameAwareEncoderTest {

    @Test
    void encode_emptyInput_producesEmptyFrame() throws FdoException {
        List<byte[]> frames = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Boolean> lastFlags = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(119, (data, index, isLast) -> {
            frames.add(data);
            indices.add(index);
            lastFlags.add(isLast);
        });

        encoder.encode(List.of());

        assertThat(frames).hasSize(1);
        assertThat(frames.get(0)).isEmpty();
        assertThat(indices.get(0)).isEqualTo(0);
        assertThat(lastFlags.get(0)).isTrue();
    }

    @Test
    void encode_singleSmallAtom_producesOneFrame() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(119, (data, index, isLast) -> {
            frames.add(data);
        });

        AtomFrame atom = new AtomFrame(0, 1, new byte[]{0x00}); // uni_start_stream <00x>
        encoder.encode(List.of(atom));

        assertThat(frames).hasSize(1);
        // FULL style: [style|proto] [atom] [len] [data] = 4 bytes
        assertThat(frames.get(0)).hasSize(4);
    }

    @Test
    void encode_multipleSmallAtoms_fitsInOneFrame() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(119, (data, index, isLast) -> {
            frames.add(data);
        });

        // Three small atoms, each 4 bytes encoded = 12 bytes total
        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[]{0x00}),
            new AtomFrame(0, 2, new byte[]{}),
            new AtomFrame(0, 3, new byte[]{0x01})
        );

        encoder.encode(atoms);

        assertThat(frames).hasSize(1);
        assertThat(frames.get(0).length).isLessThanOrEqualTo(119);
    }

    @Test
    void encode_atomsExceedingFrameSize_producesMultipleFrames() throws FdoException {
        List<byte[]> frames = new ArrayList<>();
        List<Boolean> lastFlags = new ArrayList<>();

        // Very small frame size to force multiple frames
        FrameAwareEncoder encoder = new FrameAwareEncoder(20, (data, index, isLast) -> {
            frames.add(data);
            lastFlags.add(isLast);
        });

        // Each atom is ~14 bytes encoded, 20-byte frame = 1 atom per frame
        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[10]),
            new AtomFrame(0, 2, new byte[10]),
            new AtomFrame(0, 3, new byte[10])
        );

        encoder.encode(atoms);

        assertThat(frames.size()).isGreaterThanOrEqualTo(3);

        // All frames except last should have isLast=false
        for (int i = 0; i < lastFlags.size() - 1; i++) {
            assertThat(lastFlags.get(i)).isFalse();
        }
        assertThat(lastFlags.get(lastFlags.size() - 1)).isTrue();
    }

    @Test
    void encode_atomsNeverSplitAcrossFrames() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        // 30-byte frame, 20-byte atoms
        FrameAwareEncoder encoder = new FrameAwareEncoder(30, (data, index, isLast) -> {
            frames.add(data);
        });

        // Two 20-byte atoms can't both fit in 30-byte frame
        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[16]), // ~20 bytes encoded
            new AtomFrame(0, 2, new byte[16])  // ~20 bytes encoded
        );

        encoder.encode(atoms);

        // Should be 2 frames, not 1 frame with a split atom
        assertThat(frames.size()).isEqualTo(2);

        // Each frame should be self-contained (not split)
        for (byte[] frame : frames) {
            assertThat(frame.length).isLessThanOrEqualTo(30);
        }
    }

    @Test
    void encode_largeAtom_usesContinuationProtocol() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(50, (data, index, isLast) -> {
            frames.add(data);
        });

        // 200-byte atom exceeds 50-byte frame limit
        byte[] largeData = new byte[200];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }
        AtomFrame largeAtom = new AtomFrame(5, 10, largeData);

        encoder.encode(List.of(largeAtom));

        // Should produce multiple frames
        assertThat(frames.size()).isGreaterThan(1);

        // First frame should contain UNI_START_LARGE_ATOM
        byte[] firstFrame = frames.get(0);
        // FULL style byte for protocol 0: 0x00
        // Atom number 4 (UNI_START_LARGE_ATOM)
        assertThat(firstFrame[0] & 0x1F).isEqualTo(0); // Protocol 0
        assertThat(firstFrame[1]).isEqualTo((byte) LargeAtomSplitter.UNI_START_LARGE_ATOM);
    }

    @Test
    void encode_frameIndicesAreSequential() throws FdoException {
        List<Integer> indices = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(20, (data, index, isLast) -> {
            indices.add(index);
        });

        // Create enough atoms to generate multiple frames
        List<AtomFrame> atoms = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            atoms.add(new AtomFrame(0, i % 32, new byte[10]));
        }

        encoder.encode(atoms);

        // Indices should be 0, 1, 2, 3, ...
        for (int i = 0; i < indices.size(); i++) {
            assertThat(indices.get(i)).isEqualTo(i);
        }
    }

    @Test
    void encode_onlyLastFrameHasIsLastTrue() throws FdoException {
        List<Boolean> lastFlags = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(20, (data, index, isLast) -> {
            lastFlags.add(isLast);
        });

        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[10]),
            new AtomFrame(0, 2, new byte[10]),
            new AtomFrame(0, 3, new byte[10]),
            new AtomFrame(0, 4, new byte[10])
        );

        encoder.encode(atoms);

        // Only the last flag should be true
        long trueCount = lastFlags.stream().filter(b -> b).count();
        assertThat(trueCount).isEqualTo(1);
        assertThat(lastFlags.get(lastFlags.size() - 1)).isTrue();
    }

    @Test
    void encode_frameSizeExactlyAtLimit() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        // Frame size that exactly fits one atom
        int exactSize = 1 + 1 + 1 + 10; // style + atom + len + 10 bytes = 13
        FrameAwareEncoder encoder = new FrameAwareEncoder(exactSize, (data, index, isLast) -> {
            frames.add(data);
        });

        AtomFrame atom = new AtomFrame(0, 1, new byte[10]);
        encoder.encode(List.of(atom));

        assertThat(frames).hasSize(1);
        assertThat(frames.get(0).length).isEqualTo(exactSize);
    }

    @Test
    void encode_mixedSmallAndLargeAtoms() throws FdoException {
        List<byte[]> frames = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(50, (data, index, isLast) -> {
            frames.add(data);
        });

        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[5]),   // Small
            new AtomFrame(0, 2, new byte[200]), // Large - needs splitting
            new AtomFrame(0, 3, new byte[5])    // Small
        );

        encoder.encode(atoms);

        // Should handle both small and large atoms correctly
        assertThat(frames.size()).isGreaterThan(3);
    }

    @Test
    void constructor_rejectsFrameSizeTooSmall() {
        assertThatThrownBy(() -> new FrameAwareEncoder(3, (d, i, l) -> {}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 4");
    }

    @Test
    void encode_p3FrameSize() throws FdoException {
        // Test with actual P3 frame size (119 bytes)
        List<byte[]> frames = new ArrayList<>();

        FrameAwareEncoder encoder = new FrameAwareEncoder(119, (data, index, isLast) -> {
            frames.add(data);
        });

        // Create atoms that would fit in a single 119-byte frame
        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[20]),
            new AtomFrame(0, 2, new byte[20]),
            new AtomFrame(0, 3, new byte[20]),
            new AtomFrame(0, 4, new byte[20])
        );

        encoder.encode(atoms);

        // Should fit in one frame: 4 * ~24 bytes = ~96 bytes < 119
        assertThat(frames).hasSize(1);
        assertThat(frames.get(0).length).isLessThanOrEqualTo(119);
    }

    @Test
    void encode_getFrameCount() throws FdoException {
        FrameAwareEncoder encoder = new FrameAwareEncoder(20, (data, index, isLast) -> {});

        List<AtomFrame> atoms = List.of(
            new AtomFrame(0, 1, new byte[10]),
            new AtomFrame(0, 2, new byte[10]),
            new AtomFrame(0, 3, new byte[10])
        );

        encoder.encode(atoms);

        assertThat(encoder.getFrameCount()).isGreaterThan(0);
    }

    @Test
    void encode_compileToFramesIntegration() throws FdoException {
        // Integration test with FdoCompiler
        FdoCompiler compiler = FdoCompiler.create();
        List<byte[]> frames = new ArrayList<>();

        String source = """
            uni_start_stream <00x>
              man_start_object <independent, "Test">
                mat_object_id <32-105>
              man_end_object
            uni_end_stream
            """;

        compiler.compileToFrames(source, 119, (data, index, isLast) -> {
            frames.add(data);
        });

        assertThat(frames).isNotEmpty();
        // All frames should be within size limit
        for (byte[] frame : frames) {
            assertThat(frame.length).isLessThanOrEqualTo(119);
        }
    }

    @Test
    void encode_compileToFramesMatchesCompile() throws FdoException {
        // Verify that concatenating frame output equals regular compile output
        FdoCompiler compiler = FdoCompiler.create();

        String source = """
            uni_start_stream <00x>
              mat_orientation <vcf>
            uni_end_stream
            """;

        // Regular compile
        byte[] regularOutput = compiler.compile(source);

        // Frame-aware compile (with large frame size so no splitting)
        List<byte[]> frames = new ArrayList<>();
        compiler.compileToFrames(source, 10000, (data, index, isLast) -> {
            frames.add(data);
        });

        // Concatenate frames
        int totalSize = frames.stream().mapToInt(f -> f.length).sum();
        byte[] frameOutput = new byte[totalSize];
        int offset = 0;
        for (byte[] frame : frames) {
            System.arraycopy(frame, 0, frameOutput, offset, frame.length);
            offset += frame.length;
        }

        assertThat(frameOutput).isEqualTo(regularOutput);
    }
}
