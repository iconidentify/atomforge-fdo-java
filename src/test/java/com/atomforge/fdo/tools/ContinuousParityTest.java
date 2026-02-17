package com.atomforge.fdo.tools;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Continuous parity testing against ADA32 API.
 * Runs all golden tests and compares outputs, tracking progress over time.
 */
public class ContinuousParityTest {

    private static final String ADA32_URL = "http://i9beef:8000/compile";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden");
    private static final Path PROGRESS_FILE = Path.of("docs/PARITY_PROGRESS.md");

    public static void main(String[] args) throws Exception {
        boolean compareWithAda32 = args.length > 0 && args[0].equals("--compare-ada32");
        
        FdoCompiler compiler = FdoCompiler.create();

        System.out.println("=".repeat(70));
        System.out.println("CONTINUOUS PARITY TEST");
        System.out.println("=".repeat(70));
        System.out.println();

        // Find all test files
        List<String> testNames = Files.list(GOLDEN_DIR)
            .filter(p -> p.toString().endsWith(".txt"))
            .map(p -> p.getFileName().toString().replace(".txt", ""))
            .sorted()
            .toList();

        System.out.println("Found " + testNames.size() + " golden tests\n");

        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger binaryMismatch = new AtomicInteger(0);
        AtomicInteger compileError = new AtomicInteger(0);
        AtomicInteger ada32Error = new AtomicInteger(0);
        AtomicInteger ada32Mismatch = new AtomicInteger(0);

        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        List<String> ada32Failures = Collections.synchronizedList(new ArrayList<>());

        // Test each file
        for (String testName : testNames) {
            Path txtFile = GOLDEN_DIR.resolve(testName + ".txt");
            Path binFile = GOLDEN_DIR.resolve(testName + ".bin");

            if (!Files.exists(binFile)) {
                continue;
            }

            try {
                String source = Files.readString(txtFile, StandardCharsets.UTF_8);
                byte[] expected = Files.readAllBytes(binFile);

                // Compile with our compiler
                byte[] ourBinary = compiler.compile(source);

                // Compare with expected
                if (Arrays.equals(ourBinary, expected)) {
                    passed.incrementAndGet();
                } else {
                    binaryMismatch.incrementAndGet();
                    failures.add(testName);
                }

                // Compare with ADA32 if requested
                if (compareWithAda32) {
                    try {
                        byte[] ada32Binary = compileViaAda32(source);
                        if (ada32Binary != null) {
                            if (!Arrays.equals(ourBinary, ada32Binary)) {
                                ada32Mismatch.incrementAndGet();
                                ada32Failures.add(testName);
                            }
                        } else {
                            ada32Error.incrementAndGet();
                        }
                    } catch (Exception e) {
                        ada32Error.incrementAndGet();
                    }
                }

            } catch (FdoException e) {
                compileError.incrementAndGet();
                failures.add(testName + " (compile error: " + e.getCode() + ")");
            } catch (Exception e) {
                compileError.incrementAndGet();
                failures.add(testName + " (error: " + e.getMessage() + ")");
            }
        }

        // Print summary
        int total = testNames.size();
        int passing = passed.get();
        double passRate = 100.0 * passing / total;

        System.out.println("=".repeat(70));
        System.out.println("RESULTS SUMMARY");
        System.out.println("=".repeat(70));
        System.out.printf("Total tests:     %d%n", total);
        System.out.printf("Passing:         %d (%.2f%%)%n", passing, passRate);
        System.out.printf("Binary mismatch: %d%n", binaryMismatch.get());
        System.out.printf("Compile errors:  %d%n", compileError.get());
        
        if (compareWithAda32) {
            System.out.printf("ADA32 mismatch:  %d%n", ada32Mismatch.get());
            System.out.printf("ADA32 errors:   %d%n", ada32Error.get());
        }

        // Update progress file
        updateProgressFile(total, passing, binaryMismatch.get(), compileError.get(), 
                          compareWithAda32 ? ada32Mismatch.get() : 0);

        // Print sample failures
        if (!failures.isEmpty()) {
            System.out.println("\nSample failures (first 10):");
            failures.stream().limit(10).forEach(f -> System.out.println("  " + f));
            if (failures.size() > 10) {
                System.out.println("  ... and " + (failures.size() - 10) + " more");
            }
        }

        if (compareWithAda32 && !ada32Failures.isEmpty()) {
            System.out.println("\nADA32 mismatches (first 10):");
            ada32Failures.stream().limit(10).forEach(f -> System.out.println("  " + f));
            if (ada32Failures.size() > 10) {
                System.out.println("  ... and " + (ada32Failures.size() - 10) + " more");
            }
        }
    }

    private static byte[] compileViaAda32(String source) throws Exception {
        String jsonBody = String.format("{\"source\": %s}", TestUtils.escapeJsonString(source));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ADA32_URL))
            .header("Content-Type", "application/json")
            .header("Accept", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(java.time.Duration.ofSeconds(30))
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            return null;
        }

        byte[] body = response.body();
        if (body.length > 0 && body[0] == '{') {
            return null;
        }

        return body;
    }

    private static void updateProgressFile(int total, int passing, int binaryMismatch, 
                                         int compileError, int ada32Mismatch) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            double passRate = 100.0 * passing / total;

            StringBuilder content = new StringBuilder();
            content.append("# Parity Progress Tracking\n\n");
            content.append("This file tracks progress toward 100% parity with ADA32.\n\n");
            content.append("## Latest Run\n\n");
            content.append("**Timestamp:** ").append(timestamp).append("\n\n");
            content.append("| Metric | Count | Percentage |\n");
            content.append("|--------|-------|------------|\n");
            content.append(String.format("| Total Tests | %d | 100.0%% |\n", total));
            content.append(String.format("| Passing | %d | %.2f%% |\n", passing, passRate));
            content.append(String.format("| Binary Mismatch | %d | %.2f%% |\n", 
                binaryMismatch, 100.0 * binaryMismatch / total));
            content.append(String.format("| Compile Errors | %d | %.2f%% |\n", 
                compileError, 100.0 * compileError / total));
            if (ada32Mismatch > 0) {
                content.append(String.format("| ADA32 Mismatch | %d | %.2f%% |\n", 
                    ada32Mismatch, 100.0 * ada32Mismatch / total));
            }
            content.append("\n## History\n\n");
            content.append("(Previous runs will be appended here)\n");

            // Append to existing file if it exists
            if (Files.exists(PROGRESS_FILE)) {
                String existing = Files.readString(PROGRESS_FILE);
                if (existing.contains("## History")) {
                    int historyIndex = existing.indexOf("## History");
                    String history = existing.substring(historyIndex);
                    content.append("\n");
                    content.append(history);
                    content.append("\n### ").append(timestamp).append("\n\n");
                    content.append(String.format("- Pass rate: %.2f%% (%d/%d)\n", passRate, passing, total));
                    content.append(String.format("- Binary mismatches: %d\n", binaryMismatch));
                    content.append(String.format("- Compile errors: %d\n", compileError));
                    if (ada32Mismatch > 0) {
                        content.append(String.format("- ADA32 mismatches: %d\n", ada32Mismatch));
                    }
                }
            }

            Files.createDirectories(PROGRESS_FILE.getParent());
            Files.writeString(PROGRESS_FILE, content.toString());
        } catch (Exception e) {
            System.err.println("Warning: Could not update progress file: " + e.getMessage());
        }
    }
}

