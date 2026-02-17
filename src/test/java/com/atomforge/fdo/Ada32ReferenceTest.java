package com.atomforge.fdo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify our Java FdoCompiler produces identical output to Ada32.dll.
 *
 * <p>Reference binaries in src/test/resources/ada32-reference/ were generated
 * by calling the Ada32 HTTP API at http://i9beef:8000/compile
 */
class Ada32ReferenceTest {

    private static final HexFormat HEX = HexFormat.of();

    /**
     * Test reference_001 matches Ada32 output.
     *
     * This tests:
     * - mat_font_sis atom
     * - mat_scroll_threshold, mat_paragraph, mat_bool_force_scroll
     * - man_append_data with string
     * - uni_wait_off_end_stream atom
     * - ornament and view object types
     */
    @Test
    void testReference001MatchesAda32() throws FdoException, IOException {
        // Load FDO source
        String source = loadTextResource("ada32-reference/reference_001.fdo.txt");

        // Load Ada32 reference binary
        byte[] expected = loadBinaryResource("ada32-reference/reference_001.bin");

        // Compile with our Java implementation
        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        // Compare
        if (!java.util.Arrays.equals(expected, actual)) {
            // Find first difference for debugging
            int diffIndex = findFirstDifference(expected, actual);
            String context = formatDiffContext(expected, actual, diffIndex);

            fail(String.format(
                "Binary mismatch at byte %d (0x%04X)%n" +
                "Expected length: %d bytes%n" +
                "Actual length: %d bytes%n%n" +
                "Context around difference:%n%s",
                diffIndex, diffIndex, expected.length, actual.length, context
            ));
        }
    }

    /**
     * Test reference_002 (capture_044) compilation matches Ada32 output.
     *
     * This is a complex FDO file with:
     * - Nested objects (triggers, views, groups)
     * - Various mat_ attributes
     * - Embedded action streams (act_replace_select_action)
     * - Multiple object types (ind_group, trigger, view, org_group)
     */
    @Test
    void testReference002MatchesAda32() throws FdoException, IOException {
        // Load FDO source
        String source = loadTextResource("livecapture/source/capture_044.fdo.txt");

        // Load Ada32 reference binary
        byte[] expected = loadBinaryResource("ada32-reference/reference_002.bin");

        // Compile with our Java implementation
        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        // Compare
        if (!java.util.Arrays.equals(expected, actual)) {
            // Find first difference for debugging
            int diffIndex = findFirstDifference(expected, actual);
            String context = formatDiffContext(expected, actual, diffIndex);

            fail(String.format(
                "Binary mismatch at byte %d (0x%04X)%n" +
                "Expected length: %d bytes%n" +
                "Actual length: %d bytes%n%n" +
                "Context around difference:%n%s",
                diffIndex, diffIndex, expected.length, actual.length, context
            ));
        }
    }

    // ========== Helper Methods ==========

    private String loadTextResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] loadBinaryResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return is.readAllBytes();
        }
    }

    private int findFirstDifference(byte[] expected, byte[] actual) {
        int minLen = Math.min(expected.length, actual.length);
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                return i;
            }
        }
        return minLen; // Difference is length mismatch
    }

    private String formatDiffContext(byte[] expected, byte[] actual, int diffIndex) {
        StringBuilder sb = new StringBuilder();

        int start = Math.max(0, diffIndex - 8);
        int endExpected = Math.min(expected.length, diffIndex + 16);
        int endActual = Math.min(actual.length, diffIndex + 16);

        sb.append(String.format("Expected (bytes %d-%d):%n", start, endExpected - 1));
        sb.append(formatHexLine(expected, start, endExpected, diffIndex));
        sb.append("\n");

        sb.append(String.format("Actual (bytes %d-%d):%n", start, endActual - 1));
        sb.append(formatHexLine(actual, start, endActual, diffIndex));

        return sb.toString();
    }

    private String formatHexLine(byte[] data, int start, int end, int highlight) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i == highlight) {
                sb.append("[");
            }
            sb.append(String.format("%02x", data[i] & 0xFF));
            if (i == highlight) {
                sb.append("]");
            }
            sb.append(" ");
        }
        return sb.toString();
    }
}
