package com.atomforge.fdo;

import com.atomforge.fdo.binary.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BinaryCodecTest {

    @Test
    void shouldDecodeStyleFull() throws Exception {
        // STYLE_FULL: [style|proto] [atom] [len] [data...]
        // Protocol 0, atom 1, length 0, no data (uni_start_stream)
        byte[] binary = {0x00, 0x01, 0x00};
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.protocol()).isEqualTo(0);
        assertThat(frame.atomNumber()).isEqualTo(1);
        assertThat(frame.dataLength()).isEqualTo(0);
    }

    @Test
    void shouldDecodeStyleAtom() throws Exception {
        // STYLE_ATOM: [0110 0001] = style 3, atom 1
        // uni_start_stream (no data)
        byte[] binary = {0x61};
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.atomNumber()).isEqualTo(1);
        assertThat(frame.dataLength()).isEqualTo(0);
        assertThat(frame.style()).isEqualTo(EncodingStyle.ATOM);
    }

    @Test
    void shouldDecodeStyleZero() throws Exception {
        // STYLE_ZERO: [1010 0001] = style 5, atom 1
        byte[] binary = {(byte) 0xA1};
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.atomNumber()).isEqualTo(1);
        assertThat(frame.dataLength()).isEqualTo(1);
        assertThat(frame.data()[0]).isEqualTo((byte) 0);
    }

    @Test
    void shouldDecodeStyleOne() throws Exception {
        // STYLE_ONE: [1100 0001] = style 6, atom 1
        byte[] binary = {(byte) 0xC1};
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.atomNumber()).isEqualTo(1);
        assertThat(frame.dataLength()).isEqualTo(1);
        assertThat(frame.data()[0]).isEqualTo((byte) 1);
    }

    @Test
    void shouldDecodeStyleData() throws Exception {
        // STYLE_DATA: [0100 0000] [data|atom] = style 2, proto 0
        // Second byte: data=5, atom=3
        byte[] binary = {0x40, (byte) 0xA3}; // 0xA3 = 101 00011 = data 5, atom 3
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.protocol()).isEqualTo(0);
        assertThat(frame.atomNumber()).isEqualTo(3);
        assertThat(frame.dataLength()).isEqualTo(1);
        assertThat(frame.data()[0]).isEqualTo((byte) 5);
    }

    @Test
    void shouldDecodeStyleLength() throws Exception {
        // STYLE_LENGTH: [0010 0000] [len|atom] [data...]
        // Second byte: len=3, atom=5 -> 0x65 = 011 00101
        byte[] binary = {0x20, 0x65, 0x01, 0x02, 0x03};
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(1);
        AtomFrame frame = frames.get(0);
        assertThat(frame.protocol()).isEqualTo(0);
        assertThat(frame.atomNumber()).isEqualTo(5);
        assertThat(frame.dataLength()).isEqualTo(3);
        assertThat(frame.data()).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    void shouldEncodeAndDecodeRoundTrip() throws Exception {
        // Create some frames
        List<AtomFrame> original = List.of(
            AtomFrame.noData(0, 1),              // uni_start_stream
            AtomFrame.singleByte(16, 12, 0x20),  // mat_object_id with data
            AtomFrame.noData(0, 2)               // uni_end_stream
        );

        // Encode
        BinaryEncoder encoder = new BinaryEncoder();
        byte[] encoded = encoder.encode(original);

        // Decode
        BinaryDecoder decoder = new BinaryDecoder(encoded);
        List<AtomFrame> decoded = decoder.decode();

        // Verify (ignoring style which may differ)
        assertThat(decoded).hasSize(3);
        assertThat(decoded.get(0).protocol()).isEqualTo(0);
        assertThat(decoded.get(0).atomNumber()).isEqualTo(1);
        assertThat(decoded.get(1).protocol()).isEqualTo(16);
        assertThat(decoded.get(1).atomNumber()).isEqualTo(12);
        assertThat(decoded.get(2).protocol()).isEqualTo(0);
        assertThat(decoded.get(2).atomNumber()).isEqualTo(2);
    }

    @Test
    void shouldDecodeMultipleFrames() throws Exception {
        // Two STYLE_FULL frames: proto 0 atom 1, proto 0 atom 2
        byte[] binary = {
            0x00, 0x01, 0x00,  // uni_start_stream
            0x00, 0x02, 0x00   // uni_end_stream
        };
        BinaryDecoder decoder = new BinaryDecoder(binary);

        List<AtomFrame> frames = decoder.decode();

        assertThat(frames).hasSize(2);
        assertThat(frames.get(0).atomNumber()).isEqualTo(1);
        assertThat(frames.get(1).atomNumber()).isEqualTo(2);
    }

    @Test
    void shouldHandleLongData() throws Exception {
        // Create frame with 100 bytes of data
        byte[] data = new byte[100];
        for (int i = 0; i < 100; i++) {
            data[i] = (byte) i;
        }
        AtomFrame frame = new AtomFrame(5, 12, data);

        BinaryEncoder encoder = new BinaryEncoder();
        byte[] encoded = encoder.encode(List.of(frame));

        BinaryDecoder decoder = new BinaryDecoder(encoded);
        List<AtomFrame> decoded = decoder.decode();

        assertThat(decoded).hasSize(1);
        assertThat(decoded.get(0).dataLength()).isEqualTo(100);
        assertThat(decoded.get(0).data()).isEqualTo(data);
    }
}
