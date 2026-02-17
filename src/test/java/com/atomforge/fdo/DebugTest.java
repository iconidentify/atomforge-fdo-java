package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

class DebugTest {
    @Test
    void debugTest1082() throws Exception {
        Path binPath = Path.of("src/test/resources/golden/test_1082.bin");
        Path txtPath = Path.of("src/test/resources/golden/test_1082.txt");

        byte[] expected = Files.readAllBytes(binPath);
        String source = Files.readString(txtPath);

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("Expected length: " + expected.length);
        System.out.println("Actual length: " + actual.length);

        int firstDiff = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff >= 0) {
            System.out.printf("First difference at offset %d (0x%04X)%n", firstDiff, firstDiff);
            System.out.printf("  Expected: 0x%02X%n", expected[firstDiff] & 0xFF);
            System.out.printf("  Actual:   0x%02X%n", actual[firstDiff] & 0xFF);

            // Show context
            int start = Math.max(0, firstDiff - 10);
            int end = Math.min(expected.length, firstDiff + 20);

            System.out.println("\nExpected context:");
            StringBuilder expCtx = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i == firstDiff) expCtx.append("[");
                expCtx.append(String.format("%02x", expected[i] & 0xFF));
                if (i == firstDiff) expCtx.append("]");
                expCtx.append(" ");
            }
            System.out.println(expCtx);

            System.out.println("Actual context:");
            StringBuilder actCtx = new StringBuilder();
            for (int i = start; i < Math.min(actual.length, end); i++) {
                if (i == firstDiff) actCtx.append("[");
                actCtx.append(String.format("%02x", actual[i] & 0xFF));
                if (i == firstDiff) actCtx.append("]");
                actCtx.append(" ");
            }
            System.out.println(actCtx);
        } else if (expected.length != actual.length) {
            System.out.println("Only difference is length!");
        } else {
            System.out.println("Files are identical!");
        }
    }

    @Test
    void debugTest1083() throws Exception {
        Path binPath = Path.of("src/test/resources/golden/test_1083.bin");
        Path txtPath = Path.of("src/test/resources/golden/test_1083.txt");

        byte[] expected = Files.readAllBytes(binPath);
        String source = Files.readString(txtPath);

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("Test 1083:");
        System.out.println("Expected length: " + expected.length);
        System.out.println("Actual length: " + actual.length);

        int firstDiff = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff >= 0) {
            System.out.printf("First difference at offset %d (0x%04X)%n", firstDiff, firstDiff);
            System.out.printf("  Expected: 0x%02X%n", expected[firstDiff] & 0xFF);
            System.out.printf("  Actual:   0x%02X%n", actual[firstDiff] & 0xFF);
        }
    }

    @Test
    void debugTest1084() throws Exception {
        Path binPath = Path.of("src/test/resources/golden/test_1084.bin");
        Path txtPath = Path.of("src/test/resources/golden/test_1084.txt");

        byte[] expected = Files.readAllBytes(binPath);
        String source = Files.readString(txtPath);

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("Test 1084:");
        System.out.println("Expected length: " + expected.length);
        System.out.println("Actual length: " + actual.length);

        int firstDiff = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff >= 0) {
            System.out.printf("First difference at offset %d (0x%04X)%n", firstDiff, firstDiff);
            System.out.printf("  Expected: 0x%02X%n", expected[firstDiff] & 0xFF);
            System.out.printf("  Actual:   0x%02X%n", actual[firstDiff] & 0xFF);
        }
    }
}
