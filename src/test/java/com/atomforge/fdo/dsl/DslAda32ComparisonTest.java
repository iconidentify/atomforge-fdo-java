package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.atoms.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify DSL-produced binary matches Ada32-compiled reference binaries.
 *
 * These tests compare:
 * - DSL Java API -> compile() -> binary
 * - Ada32 reference binary (.compiled.bin)
 *
 * This proves the DSL produces byte-identical output to the original Ada32 compiler.
 */
@DisplayName("DSL vs Ada32 Binary Comparison")
class DslAda32ComparisonTest {

    /**
     * Test 32-572: Simple sound playback
     *
     * Original FDO source:
     * <pre>
     * uni_start_stream <>
     *   async_play_sound_dammit <"You've Got Mail">
     * uni_end_stream <>
     * </pre>
     */
    @Test
    @DisplayName("32-572: async_play_sound_dammit matches Ada32")
    void test32_572_PlaySound() throws Exception {
        // Build using DSL - explicitly include stream wrappers as in FDO source
        byte[] dslBinary = FdoScript.stream()
            .uni(UniAtom.START_STREAM)
            .atom(AsyncAtom.PLAY_SOUND_DAMMIT, "You've Got Mail")
            .uni(UniAtom.END_STREAM)
            .compile();

        // Load Ada32 reference binary
        byte[] ada32Binary = loadAda32Binary("32-572");

        // Compare
        assertArrayEquals(ada32Binary, dslBinary,
            () -> buildMismatchMessage("32-572", ada32Binary, dslBinary));
    }

    /**
     * Test 32-70: Data extraction with buffer
     *
     * Original FDO source:
     * <pre>
     * uni_start_stream <>
     *   de_start_extraction <>
     *   buf_set_token <eT>
     *   man_set_context_relative <12>
     *   de_get_data <33>
     *   man_end_context <>
     *   de_end_extraction <>
     *   buf_close_buffer <>
     * uni_end_stream <>
     * </pre>
     */
    @Test
    @DisplayName("32-70: Data extraction matches Ada32")
    void test32_70_DataExtraction() throws Exception {
        // Build using DSL - explicitly include stream wrappers as in FDO source
        byte[] dslBinary = FdoScript.stream()
            .uni(UniAtom.START_STREAM)
            .de(DeAtom.START_EXTRACTION)
            .atom(BufAtom.SET_TOKEN, "eT")
            .man(ManAtom.SET_CONTEXT_RELATIVE, 12)
            .de(DeAtom.GET_DATA, 33)
            .man(ManAtom.END_CONTEXT)
            .de(DeAtom.END_EXTRACTION)
            .atom(BufAtom.CLOSE_BUFFER)
            .uni(UniAtom.END_STREAM)
            .compile();

        // Load Ada32 reference binary
        byte[] ada32Binary = loadAda32Binary("32-70");

        // Compare
        assertArrayEquals(ada32Binary, dslBinary,
            () -> buildMismatchMessage("32-70", ada32Binary, dslBinary));
    }

    // ==================== Helper Methods ====================

    private byte[] loadAda32Binary(String name) throws Exception {
        String path = "/golden_ada32/" + name + ".fdo.compiled.bin";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, "Ada32 reference binary not found: " + path);
            return is.readAllBytes();
        }
    }

    private String buildMismatchMessage(String testName, byte[] expected, byte[] actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Binary mismatch for ").append(testName).append("\n");
        sb.append("Ada32 size: ").append(expected.length).append(" bytes\n");
        sb.append("DSL size:   ").append(actual.length).append(" bytes\n");
        sb.append("\nAda32: ").append(hex(expected)).append("\n");
        sb.append("DSL:   ").append(hex(actual)).append("\n");

        // Find first difference
        int minLen = Math.min(expected.length, actual.length);
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                sb.append("\nFirst diff at offset ").append(i).append(" (0x")
                  .append(Integer.toHexString(i)).append(")\n");
                sb.append("  Ada32: 0x").append(String.format("%02X", expected[i] & 0xFF)).append("\n");
                sb.append("  DSL:   0x").append(String.format("%02X", actual[i] & 0xFF)).append("\n");
                break;
            }
        }

        return sb.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
