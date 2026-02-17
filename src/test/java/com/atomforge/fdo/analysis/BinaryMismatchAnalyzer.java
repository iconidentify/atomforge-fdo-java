package com.atomforge.fdo.analysis;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Analyzes binary mismatches between AtomForge Java compiler and Ada32 reference.
 * Run with: mvn test -Dtest=BinaryMismatchAnalyzer
 */
public class BinaryMismatchAnalyzer {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");

    public static void main(String[] args) throws Exception {
        new BinaryMismatchAnalyzer().analyze();
    }

    public void analyze() throws Exception {
        FdoCompiler compiler = FdoCompiler.create();

        // Find all golden test files
        List<String> testNames = Files.list(GOLDEN_DIR)
            .filter(p -> p.toString().endsWith(".txt"))
            .map(p -> p.getFileName().toString().replace(".txt", ""))
            .sorted()
            .collect(Collectors.toList());

        System.out.println("Analyzing " + testNames.size() + " golden tests...\n");

        Map<String, List<MismatchInfo>> categoryMap = new LinkedHashMap<>();
        Map<String, Integer> atomAtDiffMap = new HashMap<>();
        List<MismatchInfo> allMismatches = new ArrayList<>();
        int passed = 0;
        int errors = 0;

        for (String testName : testNames) {
            Path txtFile = GOLDEN_DIR.resolve(testName + ".txt");
            Path binFile = GOLDEN_DIR.resolve(testName + ".bin");

            if (!Files.exists(binFile)) continue;

            String source = Files.readString(txtFile);
            byte[] expected = Files.readAllBytes(binFile);

            try {
                byte[] actual = compiler.compile(source);

                if (Arrays.equals(expected, actual)) {
                    passed++;
                    continue;
                }

                MismatchInfo info = analyzeMismatch(testName, source, expected, actual);
                allMismatches.add(info);

                categoryMap.computeIfAbsent(info.category, k -> new ArrayList<>()).add(info);

                if (info.atomAtDiff != null) {
                    atomAtDiffMap.merge(info.atomAtDiff, 1, Integer::sum);
                }

            } catch (FdoException e) {
                errors++;
                // Skip unknown atom errors etc.
            } catch (Exception e) {
                errors++;
                // Skip other errors (parsing issues, etc.)
            }
        }

        // Print results
        System.out.println("=" .repeat(70));
        System.out.println("BINARY MISMATCH ANALYSIS REPORT");
        System.out.println("=" .repeat(70));
        System.out.println();
        System.out.printf("Total tests: %d%n", testNames.size());
        System.out.printf("Passed: %d%n", passed);
        System.out.printf("Binary mismatches: %d%n", allMismatches.size());
        System.out.printf("Compile errors: %d%n", errors);

        System.out.println("\n" + "-".repeat(70));
        System.out.println("MISMATCHES BY CATEGORY (sorted by count)");
        System.out.println("-".repeat(70));

        categoryMap.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .forEach(e -> {
                System.out.printf("\n[%s] - %d tests%n", e.getKey(), e.getValue().size());
                e.getValue().stream().limit(3).forEach(info -> {
                    System.out.printf("  %s @ offset %d: expected 0x%02X, got 0x%02X%n",
                        info.testName, info.diffOffset,
                        info.expectedByte & 0xFF, info.actualByte & 0xFF);
                    if (info.contextLine != null) {
                        System.out.printf("    Near: %s%n", info.contextLine.trim());
                    }
                });
            });

        System.out.println("\n" + "-".repeat(70));
        System.out.println("ATOMS MOST FREQUENTLY AT DIFF LOCATION");
        System.out.println("-".repeat(70));

        atomAtDiffMap.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(20)
            .forEach(e -> System.out.printf("  %-40s %d%n", e.getKey(), e.getValue()));

        // Detailed samples for top categories
        System.out.println("\n" + "-".repeat(70));
        System.out.println("DETAILED SAMPLES FOR TOP ISSUES");
        System.out.println("-".repeat(70));

        categoryMap.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .limit(5)
            .forEach(e -> {
                System.out.printf("\n=== %s (%d occurrences) ===%n", e.getKey(), e.getValue().size());
                MismatchInfo sample = e.getValue().get(0);
                System.out.printf("Sample: %s%n", sample.testName);
                System.out.printf("Diff at offset: %d (0x%04X)%n", sample.diffOffset, sample.diffOffset);
                System.out.printf("Expected length: %d, Actual length: %d%n",
                    sample.expectedLen, sample.actualLen);
                System.out.println("Expected context (hex):");
                System.out.println("  " + sample.expectedContext);
                System.out.println("Actual context (hex):");
                System.out.println("  " + sample.actualContext);
                if (sample.contextLine != null) {
                    System.out.println("Source near diff:");
                    System.out.println("  " + sample.contextLine.trim());
                }
            });

        // Output JSON for further analysis
        System.out.println("\n" + "-".repeat(70));
        System.out.println("SUMMARY FOR ARCHITECTURE TEAM");
        System.out.println("-".repeat(70));
        System.out.println("\nTop issues to fix (by impact):");
        int rank = 1;
        for (var entry : categoryMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(10)
                .toList()) {
            System.out.printf("%d. %s - %d tests (%.1f%% of mismatches)%n",
                rank++, entry.getKey(), entry.getValue().size(),
                100.0 * entry.getValue().size() / allMismatches.size());
        }
    }

    private MismatchInfo analyzeMismatch(String testName, String source,
                                         byte[] expected, byte[] actual) {
        MismatchInfo info = new MismatchInfo();
        info.testName = testName;
        info.expectedLen = expected.length;
        info.actualLen = actual.length;

        // Find first difference
        int minLen = Math.min(expected.length, actual.length);
        int diffIdx = minLen; // Default to length diff
        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                diffIdx = i;
                break;
            }
        }

        info.diffOffset = diffIdx;
        info.expectedByte = diffIdx < expected.length ? expected[diffIdx] : 0;
        info.actualByte = diffIdx < actual.length ? actual[diffIdx] : 0;

        // Context around diff
        int ctxStart = Math.max(0, diffIdx - 8);
        int ctxEnd = Math.min(expected.length, diffIdx + 8);
        info.expectedContext = formatHex(expected, ctxStart, ctxEnd, diffIdx);

        ctxEnd = Math.min(actual.length, diffIdx + 8);
        info.actualContext = formatHex(actual, ctxStart, ctxEnd, diffIdx);

        // Categorize the difference
        info.category = categorizeDiff(expected, actual, diffIdx);

        // Try to find which atom/line corresponds to this offset
        info.atomAtDiff = findAtomAtOffset(source, expected, diffIdx);
        info.contextLine = findSourceContext(source, diffIdx, expected);

        return info;
    }

    private String formatHex(byte[] data, int start, int end, int highlight) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end && i < data.length; i++) {
            if (i == highlight) sb.append("[");
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i == highlight) sb.append("]");
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String categorizeDiff(byte[] expected, byte[] actual, int diffIdx) {
        if (expected.length != actual.length) {
            int diff = actual.length - expected.length;
            if (Math.abs(diff) == 1) return "LENGTH_OFF_BY_1";
            if (Math.abs(diff) == 2) return "LENGTH_OFF_BY_2";
            if (Math.abs(diff) <= 4) return "LENGTH_OFF_BY_SMALL";
            return "LENGTH_SIGNIFICANT_DIFF";
        }

        if (diffIdx >= expected.length) return "IDENTICAL";

        int expByte = expected[diffIdx] & 0xFF;
        int actByte = actual[diffIdx] & 0xFF;
        int byteDiff = (actByte - expByte + 256) % 256;

        // Check for encoding style differences (top 3 bits)
        if (diffIdx > 0) {
            int expStyle = (expected[diffIdx] >> 5) & 0x07;
            int actStyle = (actual[diffIdx] >> 5) & 0x07;
            if (expStyle != actStyle && (expByte & 0x1F) == (actByte & 0x1F)) {
                return "ENCODING_STYLE_" + expStyle + "_VS_" + actStyle;
            }
        }

        // Check for common byte differences
        if (byteDiff == 1 || byteDiff == 255) return "VALUE_OFF_BY_1";
        if (byteDiff == 2 || byteDiff == 254) return "VALUE_OFF_BY_2";

        // Check if it looks like a GID encoding issue (4-byte vs 3-byte)
        if (diffIdx + 3 < expected.length && diffIdx + 3 < actual.length) {
            // Look for 00 00 padding that shouldn't be there
            if (actual[diffIdx] == 0 && actual[diffIdx+1] == 0) {
                return "POSSIBLE_GID_PADDING";
            }
        }

        // Check for protocol byte issues
        if (diffIdx > 0) {
            int prevExp = expected[diffIdx-1] & 0xFF;
            int prevAct = actual[diffIdx-1] & 0xFF;
            if ((prevExp & 0xE0) != (prevAct & 0xE0)) {
                return "FRAME_HEADER_STYLE";
            }
        }

        return "VALUE_MISMATCH_0x" + String.format("%02X", expByte) +
               "_vs_0x" + String.format("%02X", actByte);
    }

    private String findAtomAtOffset(String source, byte[] binary, int offset) {
        // This is approximate - try to find which atom produced bytes around offset
        String[] lines = source.split("\n");
        Pattern atomPattern = Pattern.compile("^\\s*([a-z_]+)\\s*<");

        // Rough estimate: each atom averages ~5-10 bytes
        int estimatedLine = offset / 7;
        if (estimatedLine < lines.length) {
            for (int i = Math.max(0, estimatedLine - 5);
                 i < Math.min(lines.length, estimatedLine + 5); i++) {
                Matcher m = atomPattern.matcher(lines[i]);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

    private String findSourceContext(String source, int offset, byte[] binary) {
        String[] lines = source.split("\n");
        // Estimate which line produced the bytes at offset
        int estimatedLine = Math.min(offset / 7, lines.length - 1);
        if (estimatedLine >= 0 && estimatedLine < lines.length) {
            return lines[estimatedLine];
        }
        return null;
    }

    static class MismatchInfo {
        String testName;
        int diffOffset;
        byte expectedByte;
        byte actualByte;
        int expectedLen;
        int actualLen;
        String expectedContext;
        String actualContext;
        String category;
        String atomAtDiff;
        String contextLine;
    }
}
