package com.atomforge.fdo.tools;

/**
 * Shared utilities for test tools.
 */
public final class TestUtils {

    private TestUtils() {
        // Utility class
    }

    /**
     * Escapes a string for JSON encoding.
     * Returns a quoted JSON string value.
     */
    public static String escapeJsonString(String s) {
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
        return sb.append("\"").toString();
    }
}
