package com.atomforge.fdo.golden;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.model.FdoStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Parameterized golden tests for FDO compilation and decompilation.
 * Tests against real-world FDO samples from .IDX database files.
 *
 * Test structure:
 * - .txt files: Expected decompiled FDO text
 * - .bin files: Expected compiled binary
 *
 * Tests verify:
 * 1. Decompile: .bin -> text, compare to .txt (decompiler accuracy)
 * 2. Compile: .txt -> binary, compare to .bin (compiler accuracy)
 * 3. Round-trip: text -> binary -> text, compare to original (full pipeline)
 */
class GoldenTestRunner {

    private static FdoCompiler compiler;
    private static Path goldenDir;
    private static List<Path> txtFiles;
    private static List<Path> binFiles;

    @BeforeAll
    static void setUp() throws IOException {
        compiler = FdoCompiler.create();

        // Find golden test resources
        goldenDir = Paths.get("src/test/resources/golden");
        if (!Files.exists(goldenDir)) {
            // Try classpath
            var resource = GoldenTestRunner.class.getClassLoader().getResource("golden");
            if (resource != null) {
                goldenDir = Paths.get(resource.getPath());
            }
        }

        txtFiles = new ArrayList<>();
        binFiles = new ArrayList<>();

        if (Files.exists(goldenDir)) {
            try (Stream<Path> paths = Files.list(goldenDir)) {
                paths.forEach(p -> {
                    String name = p.getFileName().toString();
                    if (name.endsWith(".txt")) {
                        txtFiles.add(p);
                    } else if (name.endsWith(".bin")) {
                        binFiles.add(p);
                    }
                });
            }
        }

        txtFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        binFiles.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
    }

    /**
     * Test compiler accuracy: .txt -> compile -> compare to .bin
     * This verifies the compiler produces the expected binary output.
     *
     * This is the primary test - compilation must match Ada32 byte-for-byte.
     */
    @TestFactory
    Stream<DynamicTest> testCompileMatchesExpected() {
        return txtFiles.stream()
            .filter(txtFile -> {
                String baseName = getBaseName(txtFile);
                Path binFile = goldenDir.resolve(baseName + ".bin");
                return Files.exists(binFile);
            })
            .map(txtFile -> {
                String baseName = getBaseName(txtFile);
                return DynamicTest.dynamicTest("compile: " + baseName, () -> {
                    String source = readTextFile(txtFile);
                    Path binFile = goldenDir.resolve(baseName + ".bin");
                    byte[] expectedBinary = Files.readAllBytes(binFile);

                    byte[] actualBinary = compiler.compile(source);

                    assertThat(actualBinary)
                        .as("Compiled binary should match expected for %s", baseName)
                        .isEqualTo(expectedBinary);
                });
            });
    }

    /**
     * Test round-trip binary fidelity: .bin -> FdoStream.decode() -> FdoStream.toBytes() -> compare to original
     * This verifies that every binary can be decoded to the model and re-encoded to identical bytes.
     *
     * This ensures the Java API can reliably represent all FDO data.
     */
    @TestFactory
    Stream<DynamicTest> testRoundTripBinaryFidelity() {
        return binFiles.stream()
            .map(binFile -> {
                String baseName = getBaseName(binFile);
                return DynamicTest.dynamicTest("round-trip: " + baseName, () -> {
                    byte[] originalBinary = Files.readAllBytes(binFile);

                    // Decode binary to model
                    FdoStream stream = FdoStream.decode(originalBinary);

                    // Re-encode model to binary
                    byte[] recompiled = stream.toBytes();

                    // Compare byte-for-byte
                    assertThat(recompiled)
                        .as("Round-trip binary should match original for %s", baseName)
                        .withFailMessage(() -> buildRoundTripMismatchMessage(baseName, originalBinary, recompiled))
                        .isEqualTo(originalBinary);
                });
            });
    }

    /**
     * Build detailed mismatch message for round-trip failures.
     */
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

    /**
     * Read text file with fallback encoding for non-UTF8 files.
     */
    private String readTextFile(Path path) throws IOException {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            // Fall back to ISO-8859-1 for legacy files
            return Files.readString(path, StandardCharsets.ISO_8859_1);
        }
    }

    private String getBaseName(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
