package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

class DetailedCompareTest {
    @Test
    void compareTest1082() throws Exception {
        compareGolden("test_1082");
    }

    @Test
    void compareTest1083() throws Exception {
        compareGolden("test_1083");
    }

    @Test
    void compareTest1084() throws Exception {
        compareGolden("test_1084");
    }

    void compareGolden(String testName) throws Exception {
        Path binPath = Path.of("src/test/resources/golden/" + testName + ".bin");
        Path txtPath = Path.of("src/test/resources/golden/" + testName + ".txt");

        byte[] expected = Files.readAllBytes(binPath);
        String source = Files.readString(txtPath);

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("Expected length: " + expected.length);
        System.out.println("Actual length: " + actual.length);
        System.out.println("Length difference: " + (actual.length - expected.length));

        // Find ALL differences
        List<int[]> diffs = new ArrayList<>();
        int minLen = Math.min(expected.length, actual.length);

        for (int i = 0; i < minLen; i++) {
            if (expected[i] != actual[i]) {
                diffs.add(new int[]{i, expected[i] & 0xFF, actual[i] & 0xFF});
            }
        }

        System.out.println("\nTotal byte differences: " + diffs.size());

        // Show first 20 differences
        System.out.println("\nFirst differences:");
        for (int i = 0; i < Math.min(20, diffs.size()); i++) {
            int[] diff = diffs.get(i);
            System.out.printf("  @%d (0x%04X): expected 0x%02X, got 0x%02X (diff=%d)%n",
                diff[0], diff[0], diff[1], diff[2], diff[2] - diff[1]);
        }

        // Look for patterns - are they all in a specific region?
        if (!diffs.isEmpty()) {
            int firstDiff = diffs.get(0)[0];
            int lastDiff = diffs.get(diffs.size() - 1)[0];
            System.out.printf("\nDifferences span from offset %d to %d%n", firstDiff, lastDiff);

            // Show context around first difference
            System.out.println("\nContext around first difference:");
            int start = Math.max(0, firstDiff - 20);
            int end = Math.min(expected.length, firstDiff + 50);

            System.out.print("Expected: ");
            for (int i = start; i < end; i++) {
                if (i == firstDiff) System.out.print("[");
                System.out.printf("%02x", expected[i] & 0xFF);
                if (i == firstDiff) System.out.print("]");
                System.out.print(" ");
            }
            System.out.println();

            System.out.print("Actual:   ");
            for (int i = start; i < Math.min(actual.length, end); i++) {
                if (i == firstDiff) System.out.print("[");
                System.out.printf("%02x", actual[i] & 0xFF);
                if (i == firstDiff) System.out.print("]");
                System.out.print(" ");
            }
            System.out.println();
        }
    }
}
