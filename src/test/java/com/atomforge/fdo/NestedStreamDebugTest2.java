package com.atomforge.fdo;

import com.atomforge.fdo.binary.BinaryEncoder;
import com.atomforge.fdo.binary.AtomFrame;
import org.junit.jupiter.api.Test;
import java.util.*;

class NestedStreamDebugTest2 {
    @Test
    void testDirectEncode() throws Exception {
        // Encode just uni_start_stream + uni_end_stream directly
        BinaryEncoder encoder = new BinaryEncoder();

        List<AtomFrame> frames = new ArrayList<>();
        frames.add(new AtomFrame(0, 1, new byte[0])); // uni_start_stream
        frames.add(new AtomFrame(0, 2, new byte[0])); // uni_end_stream

        byte[] result = encoder.encode(frames);

        System.out.println("Nested stream encoding:");
        System.out.println("  Length: " + result.length);
        System.out.println("  Bytes: " + toHex(result));

        // Expected: 00 01 00 00 02 00 (6 bytes)
        // Or with FULL style: something longer
    }

    @Test
    void testActReplaceSelectActionNested() throws Exception {
        // Test the full nested case
        BinaryEncoder encoder = new BinaryEncoder();

        // First encode the nested stream
        List<AtomFrame> nestedFrames = new ArrayList<>();
        nestedFrames.add(new AtomFrame(0, 1, new byte[0])); // uni_start_stream
        nestedFrames.add(new AtomFrame(0, 2, new byte[0])); // uni_end_stream

        byte[] nestedData = encoder.encode(nestedFrames);
        System.out.println("Nested stream data:");
        System.out.println("  Length: " + nestedData.length);
        System.out.println("  Bytes: " + toHex(nestedData));

        // Now encode the outer stream
        encoder = new BinaryEncoder(); // Reset

        List<AtomFrame> outerFrames = new ArrayList<>();
        outerFrames.add(new AtomFrame(0, 1, new byte[0])); // uni_start_stream
        outerFrames.add(new AtomFrame(2, 4, nestedData));  // act_replace_select_action with nested data
        outerFrames.add(new AtomFrame(0, 2, new byte[0])); // uni_end_stream

        byte[] result = encoder.encode(outerFrames);

        System.out.println("\nFull outer stream:");
        System.out.println("  Length: " + result.length);
        System.out.println("  Bytes: " + toHex(result));

        // Expected from Ada32: 00 01 00 02 04 06 00 01 00 00 02 00 02 00 (14 bytes)
        System.out.println("\nExpected: 00 01 00 02 04 06 00 01 00 00 02 00 02 00");
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
