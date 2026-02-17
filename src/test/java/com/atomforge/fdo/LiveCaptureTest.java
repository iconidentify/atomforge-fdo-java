package com.atomforge.fdo;

import com.atomforge.fdo.model.FdoStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.HexFormat;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live Capture Tests - Production FDO scripts from Dialtone server.
 *
 * These tests validate that AtomForge produces byte-for-byte identical
 * binary output to the Ada32 reference compiler for real production scripts.
 *
 * Test data:
 * - src/test/resources/livecapture/source/*.fdo.txt - FDO source scripts
 * - src/test/resources/livecapture/binary/*.bin - Ada32 reference binaries
 */
@DisplayName("Live Capture Compilation Tests")
public class LiveCaptureTest {

    private static FdoCompiler compiler;

    @BeforeAll
    static void setup() {
        compiler = FdoCompiler.create();
    }

    /**
     * All 44 production FDO scripts from live capture.
     */
    static Stream<String> liveCaptureTestCases() {
        return Stream.of(
            "capture_001",
            "capture_002",
            "capture_003",
            "capture_004",
            "capture_005",
            "capture_006",
            "capture_007",
            "capture_008",
            "capture_009",
            "capture_010",
            "capture_011",
            "capture_012",
            "capture_013",
            "capture_014",
            "capture_015",
            "capture_016",
            "capture_017",
            "capture_018",
            "capture_019",
            "capture_020",
            "capture_021",
            "capture_022",
            "capture_023",
            "capture_024",
            "capture_025",
            "capture_026",
            "capture_027",
            "capture_028",
            "capture_029",
            "capture_030",
            "capture_031",
            "capture_032",
            "capture_033",
            "capture_034",
            "capture_035",
            "capture_036",
            "capture_037",
            "capture_038",
            "capture_039",
            "capture_040",
            "capture_041",
            "capture_042",
            "capture_043",
            "capture_044"
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("liveCaptureTestCases")
    @DisplayName("Compile matches Ada32 binary")
    void testCompileMatchesAda32(String testName) throws Exception {
        // Load FDO source
        String sourcePath = "/livecapture/source/" + testName + ".fdo.txt";
        String source;
        try (InputStream is = getClass().getResourceAsStream(sourcePath)) {
            assertNotNull(is, "Source file not found: " + sourcePath);
            source = new String(is.readAllBytes());
        }

        // Load Ada32 reference binary
        String binaryPath = "/livecapture/binary/" + testName + ".bin";
        byte[] expectedBinary;
        try (InputStream is = getClass().getResourceAsStream(binaryPath)) {
            assertNotNull(is, "Reference binary not found: " + binaryPath);
            expectedBinary = is.readAllBytes();
        }

        // Compile with AtomForge
        byte[] actualBinary = compiler.compile(source);

        // Compare byte-for-byte
        assertArrayEquals(expectedBinary, actualBinary,
            () -> buildMismatchMessage(testName, expectedBinary, actualBinary));
    }

    private String buildMismatchMessage(String testName, byte[] expected, byte[] actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Binary mismatch for ").append(testName).append("\n");
        sb.append("Expected size: ").append(expected.length).append(" bytes\n");
        sb.append("Actual size:   ").append(actual.length).append(" bytes\n");

        // Find first difference
        int minLen = Math.min(expected.length, actual.length);
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                sb.append("First diff at offset ").append(i).append(" (0x")
                  .append(Integer.toHexString(i)).append(")\n");
                sb.append("  Expected: 0x").append(String.format("%02X", expected[i] & 0xFF)).append("\n");
                sb.append("  Actual:   0x").append(String.format("%02X", actual[i] & 0xFF)).append("\n");
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Test round-trip binary fidelity: .bin -> FdoStream.decode() -> FdoStream.toBytes() -> compare to original
     * This verifies that production binaries can be decoded to the model and re-encoded to identical bytes.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("liveCaptureTestCases")
    @DisplayName("Round-trip binary fidelity")
    void testRoundTripBinaryFidelity(String testName) throws Exception {
        // Load Ada32 reference binary
        String binaryPath = "/livecapture/binary/" + testName + ".bin";
        byte[] originalBinary;
        try (InputStream is = getClass().getResourceAsStream(binaryPath)) {
            assertNotNull(is, "Reference binary not found: " + binaryPath);
            originalBinary = is.readAllBytes();
        }

        // Decode binary to model
        FdoStream stream = FdoStream.decode(originalBinary);

        // Re-encode model to binary
        byte[] recompiled = stream.toBytes();

        // Compare byte-for-byte
        assertArrayEquals(originalBinary, recompiled,
            () -> buildRoundTripMismatchMessage(testName, originalBinary, recompiled));
    }

    private String buildRoundTripMismatchMessage(String testName, byte[] expected, byte[] actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("Round-trip binary mismatch for ").append(testName).append("\n");
        sb.append("Original size: ").append(expected.length).append(" bytes\n");
        sb.append("Recompiled size: ").append(actual.length).append(" bytes\n");

        // Find first difference
        int minLen = Math.min(expected.length, actual.length);
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                sb.append("First diff at offset ").append(i).append(" (0x")
                  .append(Integer.toHexString(i)).append(")\n");
                sb.append("  Expected: 0x").append(String.format("%02X", expected[i] & 0xFF)).append("\n");
                sb.append("  Actual:   0x").append(String.format("%02X", actual[i] & 0xFF)).append("\n");

                // Show context around the difference
                int start = Math.max(0, i - 4);
                int end = Math.min(minLen, i + 8);
                HexFormat hex = HexFormat.of();
                sb.append("  Context expected: ").append(hex.formatHex(expected, start, end)).append("\n");
                sb.append("  Context actual:   ").append(hex.formatHex(actual, start, end)).append("\n");
                break;
            }
        }

        if (expected.length != actual.length) {
            sb.append("Size difference: ").append(actual.length - expected.length).append(" bytes\n");
        }

        return sb.toString();
    }
}
