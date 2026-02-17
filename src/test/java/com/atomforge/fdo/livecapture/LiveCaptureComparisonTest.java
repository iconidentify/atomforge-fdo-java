package com.atomforge.fdo.livecapture;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compares our FdoCompiler output against Ada32 API for live capture scripts.
 *
 * This tool:
 * 1. Reads each extracted FDO script
 * 2. Compiles with our FdoCompiler
 * 3. Sends to Ada32 API and gets reference binary
 * 4. Compares byte-by-byte
 * 5. Generates a detailed report
 *
 * Usage:
 *   java LiveCaptureComparisonTest [--capture] [--compare] [--report]
 *
 *   --capture : Fetch binaries from Ada32 API (requires network access to i9beef:8000)
 *   --compare : Compare our output vs Ada32 output
 *   --report  : Generate detailed report (default if no args)
 */
public class LiveCaptureComparisonTest {

    private static final String ADA32_API = "http://i9beef:8000/compile";
    private static final Path EXTRACTED_DIR = Path.of("livecapture_reearch/extracted");
    private static final Path ADA32_DIR = Path.of("livecapture_reearch/ada32_binary");
    private static final Path ATOMFORGE_DIR = Path.of("livecapture_reearch/atomforge_binary");

    private final FdoCompiler compiler;
    private final HttpClient httpClient;

    public LiveCaptureComparisonTest() {
        this.compiler = FdoCompiler.create();
        this.httpClient = HttpClient.newHttpClient();
    }

    public static void main(String[] args) throws Exception {
        LiveCaptureComparisonTest test = new LiveCaptureComparisonTest();

        Set<String> flags = new HashSet<>(Arrays.asList(args));

        if (flags.isEmpty() || flags.contains("--report")) {
            flags.add("--compare");
        }

        if (flags.contains("--capture")) {
            test.captureAda32Binaries();
        }

        if (flags.contains("--compile")) {
            test.compileWithAtomforge();
        }

        if (flags.contains("--compare")) {
            test.runComparison();
        }
    }

    /**
     * Fetch compiled binaries from Ada32 API for all extracted scripts.
     */
    public void captureAda32Binaries() throws Exception {
        System.out.println("=== Capturing Ada32 API Binaries ===\n");

        Files.createDirectories(ADA32_DIR);

        List<Path> scripts = listScripts();
        int success = 0;
        int failed = 0;

        for (Path script : scripts) {
            String source = Files.readString(script);
            String baseName = script.getFileName().toString().replace(".fdo.txt", "");
            Path outputPath = ADA32_DIR.resolve(baseName + ".bin");

            try {
                byte[] binary = compileWithAda32(source);
                Files.write(outputPath, binary);
                System.out.printf("  [OK] %s -> %d bytes%n", baseName, binary.length);
                success++;
            } catch (Exception e) {
                System.out.printf("  [FAIL] %s: %s%n", baseName, e.getMessage());
                failed++;
            }
        }

        System.out.printf("%nCapture complete: %d success, %d failed%n", success, failed);
    }

    /**
     * Compile all extracted scripts with our AtomForge compiler.
     */
    public void compileWithAtomforge() throws Exception {
        System.out.println("=== Compiling with AtomForge ===\n");

        Files.createDirectories(ATOMFORGE_DIR);

        List<Path> scripts = listScripts();
        int success = 0;
        int failed = 0;

        for (Path script : scripts) {
            String source = Files.readString(script);
            String baseName = script.getFileName().toString().replace(".fdo.txt", "");
            Path outputPath = ATOMFORGE_DIR.resolve(baseName + ".bin");

            try {
                byte[] binary = compiler.compile(source);
                Files.write(outputPath, binary);
                System.out.printf("  [OK] %s -> %d bytes%n", baseName, binary.length);
                success++;
            } catch (FdoException e) {
                System.out.printf("  [FAIL] %s: %s%n", baseName, e.getMessage());
                // Write error marker
                Files.writeString(outputPath.resolveSibling(baseName + ".error"),
                    "Compilation failed: " + e.getMessage());
                failed++;
            }
        }

        System.out.printf("%nCompile complete: %d success, %d failed%n", success, failed);
    }

    /**
     * Compare AtomForge output against Ada32 reference binaries.
     */
    public void runComparison() throws Exception {
        System.out.println("=== Live Capture Comparison Report ===\n");

        List<Path> scripts = listScripts();
        List<ComparisonResult> results = new ArrayList<>();

        int match = 0;
        int mismatch = 0;
        int missing = 0;
        int compileError = 0;

        for (Path script : scripts) {
            String baseName = script.getFileName().toString().replace(".fdo.txt", "");
            Path ada32Path = ADA32_DIR.resolve(baseName + ".bin");
            Path atomforgePath = ATOMFORGE_DIR.resolve(baseName + ".bin");
            Path errorPath = ATOMFORGE_DIR.resolve(baseName + ".error");

            ComparisonResult result = new ComparisonResult(baseName);

            // Check if AtomForge compilation failed
            if (Files.exists(errorPath)) {
                result.atomforgeError = Files.readString(errorPath);
                results.add(result);
                compileError++;
                continue;
            }

            // Check for missing files
            if (!Files.exists(ada32Path)) {
                result.ada32Missing = true;
                missing++;
            } else {
                result.ada32Binary = Files.readAllBytes(ada32Path);
            }

            if (!Files.exists(atomforgePath)) {
                // Try to compile on the fly
                try {
                    String source = Files.readString(script);
                    result.atomforgeBinary = compiler.compile(source);
                    Files.write(atomforgePath, result.atomforgeBinary);
                } catch (FdoException e) {
                    result.atomforgeError = e.getMessage();
                    compileError++;
                    results.add(result);
                    continue;
                }
            } else {
                result.atomforgeBinary = Files.readAllBytes(atomforgePath);
            }

            // Compare if both exist
            if (result.ada32Binary != null && result.atomforgeBinary != null) {
                if (Arrays.equals(result.ada32Binary, result.atomforgeBinary)) {
                    result.matches = true;
                    match++;
                } else {
                    result.findFirstDifference();
                    mismatch++;
                }
            }

            results.add(result);
        }

        // Print summary
        System.out.println("SUMMARY");
        System.out.println("=======");
        System.out.printf("Total scripts:     %d%n", scripts.size());
        System.out.printf("Exact match:       %d (%.1f%%)%n", match, 100.0 * match / scripts.size());
        System.out.printf("Mismatch:          %d%n", mismatch);
        System.out.printf("Compile error:     %d%n", compileError);
        System.out.printf("Missing Ada32:     %d%n", missing);
        System.out.println();

        // Print details for mismatches
        if (mismatch > 0 || compileError > 0) {
            System.out.println("ISSUES");
            System.out.println("======");

            for (ComparisonResult r : results) {
                if (r.atomforgeError != null) {
                    System.out.printf("%n[COMPILE ERROR] %s%n", r.name);
                    System.out.printf("  Error: %s%n", r.atomforgeError);
                } else if (!r.matches && r.ada32Binary != null && r.atomforgeBinary != null) {
                    System.out.printf("%n[MISMATCH] %s%n", r.name);
                    System.out.printf("  Ada32 size:     %d bytes%n", r.ada32Binary.length);
                    System.out.printf("  AtomForge size: %d bytes%n", r.atomforgeBinary.length);
                    if (r.firstDiffOffset >= 0) {
                        System.out.printf("  First diff at:  offset %d%n", r.firstDiffOffset);
                        System.out.printf("  Ada32:          0x%02X%n", r.ada32ByteAtDiff);
                        System.out.printf("  AtomForge:      0x%02X%n", r.atomforgeByteAtDiff);
                    }
                }
            }
        }

        // Print matches
        if (match > 0) {
            System.out.println("\nMATCHES");
            System.out.println("=======");
            for (ComparisonResult r : results) {
                if (r.matches) {
                    System.out.printf("  [OK] %s (%d bytes)%n", r.name,
                        r.atomforgeBinary != null ? r.atomforgeBinary.length : 0);
                }
            }
        }
    }

    /**
     * Compile FDO source using the Ada32 API.
     */
    private byte[] compileWithAda32(String source) throws Exception {
        String json = "{\"source\": " + escapeJson(source) + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ADA32_API))
            .header("Content-Type", "application/json")
            .header("Accept", "application/octet-stream")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<byte[]> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("Ada32 API returned " + response.statusCode());
        }

        return response.body();
    }

    private String escapeJson(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private List<Path> listScripts() throws IOException {
        try (var stream = Files.list(EXTRACTED_DIR)) {
            return stream
                .filter(p -> p.toString().endsWith(".fdo.txt"))
                .sorted()
                .toList();
        }
    }

    static class ComparisonResult {
        String name;
        byte[] ada32Binary;
        byte[] atomforgeBinary;
        String atomforgeError;
        boolean ada32Missing;
        boolean matches;
        int firstDiffOffset = -1;
        int ada32ByteAtDiff;
        int atomforgeByteAtDiff;

        ComparisonResult(String name) {
            this.name = name;
        }

        void findFirstDifference() {
            if (ada32Binary == null || atomforgeBinary == null) return;

            int minLen = Math.min(ada32Binary.length, atomforgeBinary.length);
            for (int i = 0; i < minLen; i++) {
                if (ada32Binary[i] != atomforgeBinary[i]) {
                    firstDiffOffset = i;
                    ada32ByteAtDiff = ada32Binary[i] & 0xFF;
                    atomforgeByteAtDiff = atomforgeBinary[i] & 0xFF;
                    return;
                }
            }

            // If we get here, difference is in length
            if (ada32Binary.length != atomforgeBinary.length) {
                firstDiffOffset = minLen;
                ada32ByteAtDiff = minLen < ada32Binary.length ? (ada32Binary[minLen] & 0xFF) : -1;
                atomforgeByteAtDiff = minLen < atomforgeBinary.length ? (atomforgeBinary[minLen] & 0xFF) : -1;
            }
        }
    }
}
