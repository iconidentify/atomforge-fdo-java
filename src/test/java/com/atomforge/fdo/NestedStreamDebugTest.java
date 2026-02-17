package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;

class NestedStreamDebugTest {
    @Test
    void testMinimalNestedStream() throws Exception {
        String source = """
            uni_start_stream
            act_replace_select_action
                <
                uni_start_stream
                uni_end_stream
                >
            uni_end_stream
            """;

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        // Expected from Ada32 API:
        // 00000000: 0001 0002 0406 0001 0000 0200 0002 00
        byte[] expected = hexToBytes("000100020406000100000200000200");

        System.out.println("Expected length: " + expected.length);
        System.out.println("Actual length: " + actual.length);

        System.out.println("Expected: " + toHex(expected));
        System.out.println("Actual:   " + toHex(actual));

        // Find first difference
        int firstDiff = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff >= 0) {
            System.out.printf("First difference at offset %d%n", firstDiff);
            System.out.printf("  Expected: 0x%02X%n", expected[firstDiff] & 0xFF);
            System.out.printf("  Actual:   0x%02X%n", actual[firstDiff] & 0xFF);
        }
    }

    @Test
    void testNestedStreamWithOptionalArg() throws Exception {
        // uni_start_stream can have optional <hex> argument
        String source = """
            uni_start_stream <00x>
            act_replace_select_action
                <
                uni_start_stream
                uni_end_stream
                >
            uni_end_stream
            """;

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("With <00x> arg:");
        System.out.println("Actual length: " + actual.length);
        System.out.println("Actual: " + toHex(actual));
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 16 == 0) sb.append("\n        ");
            else if (i > 0) sb.append(" ");
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
