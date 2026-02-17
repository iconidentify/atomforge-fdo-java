package com.atomforge.fdo.golden;

import com.atomforge.fdo.FdoCompiler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Parameterized tests for master_atom_list.fdo - comprehensive atom-by-atom testing.
 *
 * This test reads individual atoms from master_atom_list.fdo and compares
 * the Java compiler output against Ada32 reference binaries stored in
 * master_atom_list.bin with index master_atom_list.idx.
 *
 * Prerequisites:
 * - Run MasterAtomReferenceGenerator first to create .bin and .idx files
 */
class MasterAtomListTest {

    private static final Path GOLDEN_DIR = Paths.get("src/test/resources/golden");
    private static final Path SOURCE_FILE = GOLDEN_DIR.resolve("master_atom_list.fdo");
    private static final Path BIN_FILE = GOLDEN_DIR.resolve("master_atom_list.bin");
    private static final Path IDX_FILE = GOLDEN_DIR.resolve("master_atom_list.idx");

    private static FdoCompiler compiler;
    private static List<String> sourceLines;
    private static Map<Integer, IndexEntry> index;
    private static RandomAccessFile binFile;

    @BeforeAll
    static void setUp() throws IOException {
        compiler = FdoCompiler.create();

        // Check required files exist
        if (!Files.exists(SOURCE_FILE)) {
            throw new IllegalStateException("Source file not found: " + SOURCE_FILE);
        }
        if (!Files.exists(BIN_FILE)) {
            throw new IllegalStateException("Binary file not found: " + BIN_FILE +
                "\nRun MasterAtomReferenceGenerator first to generate reference data.");
        }
        if (!Files.exists(IDX_FILE)) {
            throw new IllegalStateException("Index file not found: " + IDX_FILE +
                "\nRun MasterAtomReferenceGenerator first to generate reference data.");
        }

        // Load source lines
        sourceLines = Files.readAllLines(SOURCE_FILE, StandardCharsets.UTF_8);

        // Load index
        index = loadIndex();

        // Open binary file for random access
        binFile = new RandomAccessFile(BIN_FILE.toFile(), "r");
    }

    /**
     * Generate one test per atom line in master_atom_list.fdo.
     */
    @TestFactory
    Stream<DynamicTest> testAllAtoms() {
        return index.entrySet().stream()
            .filter(e -> !e.getValue().isError && e.getValue().length > 0)
            .sorted(Comparator.comparingInt(Map.Entry::getKey))
            .map(entry -> {
                int lineNum = entry.getKey();
                IndexEntry idx = entry.getValue();
                String source = sourceLines.get(lineNum - 1).trim();
                String atomName = extractAtomName(source);

                return DynamicTest.dynamicTest(
                    String.format("L%05d: %s", lineNum, truncate(source, 60)),
                    () -> testAtom(lineNum, source, idx)
                );
            });
    }

    private void testAtom(int lineNum, String source, IndexEntry idx) throws Exception {
        // Read expected binary from consolidated file
        byte[] expected = new byte[idx.length];
        synchronized (binFile) {
            binFile.seek(idx.offset);
            binFile.readFully(expected);
        }

        // Compile with Java compiler
        byte[] actual;
        try {
            actual = compiler.compile(source);
        } catch (Exception e) {
            fail("Compilation failed for line " + lineNum + ": " + source + "\n" + e.getMessage());
            return;
        }

        // Compare
        if (!Arrays.equals(expected, actual)) {
            String diff = buildDiffMessage(expected, actual, source, lineNum);
            fail(diff);
        }
    }

    private String buildDiffMessage(byte[] expected, byte[] actual, String source, int lineNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mismatch at line ").append(lineNum).append(": ").append(source).append("\n");
        sb.append("Expected length: ").append(expected.length).append("\n");
        sb.append("Actual length: ").append(actual.length).append("\n");

        // Find first difference
        int firstDiff = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff == -1 && expected.length != actual.length) {
            firstDiff = Math.min(expected.length, actual.length);
        }

        if (firstDiff >= 0) {
            sb.append("First diff at byte ").append(firstDiff);
            if (firstDiff < expected.length) {
                sb.append(": expected 0x").append(String.format("%02X", expected[firstDiff] & 0xFF));
            } else {
                sb.append(": expected end of data");
            }
            if (firstDiff < actual.length) {
                sb.append(", got 0x").append(String.format("%02X", actual[firstDiff] & 0xFF));
            } else {
                sb.append(", got end of data");
            }
            sb.append("\n");

            // Show context around difference
            int start = Math.max(0, firstDiff - 5);
            int end = Math.min(Math.max(expected.length, actual.length), firstDiff + 10);

            sb.append("Expected: ");
            for (int i = start; i < Math.min(end, expected.length); i++) {
                if (i == firstDiff) sb.append("[");
                sb.append(String.format("%02X", expected[i] & 0xFF));
                if (i == firstDiff) sb.append("]");
                sb.append(" ");
            }
            sb.append("\n");

            sb.append("Actual:   ");
            for (int i = start; i < Math.min(end, actual.length); i++) {
                if (i == firstDiff) sb.append("[");
                sb.append(String.format("%02X", actual[i] & 0xFF));
                if (i == firstDiff) sb.append("]");
                sb.append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static Map<Integer, IndexEntry> loadIndex() throws IOException {
        Map<Integer, IndexEntry> result = new HashMap<>();

        for (String line : Files.readAllLines(IDX_FILE, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("E,")) {
                // Error entry: E,line_number,error_message
                String[] parts = line.substring(2).split(",", 2);
                int lineNum = Integer.parseInt(parts[0]);
                String errorMsg = parts.length > 1 ? parts[1] : "Unknown error";
                result.put(lineNum, new IndexEntry(lineNum, 0, 0, true, errorMsg));
            } else {
                // Success entry: line_number,offset,length
                String[] parts = line.split(",");
                int lineNum = Integer.parseInt(parts[0]);
                long offset = Long.parseLong(parts[1]);
                int length = Integer.parseInt(parts[2]);
                result.put(lineNum, new IndexEntry(lineNum, offset, length, false, null));
            }
        }

        return result;
    }

    private static String extractAtomName(String source) {
        int space = source.indexOf(' ');
        int angle = source.indexOf('<');
        if (space < 0 && angle < 0) return source;
        if (space < 0) return source.substring(0, angle).trim();
        if (angle < 0) return source.substring(0, space).trim();
        return source.substring(0, Math.min(space, angle)).trim();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    private static class IndexEntry {
        final int lineNumber;
        final long offset;
        final int length;
        final boolean isError;
        final String errorMessage;

        IndexEntry(int lineNumber, long offset, int length, boolean isError, String errorMessage) {
            this.lineNumber = lineNumber;
            this.offset = offset;
            this.length = length;
            this.isError = isError;
            this.errorMessage = errorMessage;
        }
    }
}
