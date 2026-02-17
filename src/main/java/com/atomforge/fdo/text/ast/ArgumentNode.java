package com.atomforge.fdo.text.ast;

import java.util.List;

/**
 * Represents an argument value in the AST.
 * Arguments can be simple values (string, number, GID) or compound (nested stream).
 */
public sealed interface ArgumentNode extends FdoNode {

    /**
     * String literal argument (e.g., "Hello World")
     */
    record StringArg(String value, int line, int column) implements ArgumentNode {}

    /**
     * Numeric argument (e.g., 7, 512)
     */
    record NumberArg(long value, int line, int column) implements ArgumentNode {}

    /**
     * Hex value argument (e.g., 00x, 07x)
     */
    record HexArg(String value, int line, int column) implements ArgumentNode {}

    /**
     * GID argument (e.g., "32-105", "1-0-1329")
     */
    record GidArg(String value, int line, int column) implements ArgumentNode {
        /**
         * Parse GID into parts.
         */
        public int[] parts() {
            String[] parts = value.split("-");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i]);
            }
            return result;
        }

        /**
         * Get type part (first number).
         */
        public int type() {
            return parts()[0];
        }

        /**
         * Get subtype part (second number for 3-part GIDs, 0 otherwise).
         */
        public int subtype() {
            int[] p = parts();
            return p.length == 3 ? p[1] : 0;
        }

        /**
         * Get ID part (last number).
         */
        public int id() {
            int[] p = parts();
            return p[p.length - 1];
        }
    }

    /**
     * Identifier argument (e.g., vcf, center, yes, no, ind_group)
     */
    record IdentifierArg(String value, int line, int column) implements ArgumentNode {}

    /**
     * Compound argument with pipe (OR) (e.g., "left | center | 4")
     * Parts can be identifiers, atom names, or numbers.
     */
    record PipedArg(List<ArgumentNode> parts, int line, int column) implements ArgumentNode {}

    /**
     * Nested stream argument (e.g., < uni_start_stream uni_end_stream >)
     * May also contain trailing raw data after atom references (e.g., <uni_command, 00x, 00x, ...>)
     */
    record NestedStreamArg(StreamNode stream, List<ArgumentNode> trailingData, int line, int column) implements ArgumentNode {
        /**
         * Create a NestedStreamArg without trailing data (backward compatibility).
         */
        public NestedStreamArg(StreamNode stream, int line, int column) {
            this(stream, List.of(), line, column);
        }
    }

    /**
     * Comma-separated list of arguments (e.g., "50, 4, 512")
     */
    record ListArg(List<ArgumentNode> elements, int line, int column) implements ArgumentNode {}

    /**
     * Object type with optional title (e.g., "ind_group, \"Title\"")
     */
    record ObjectTypeArg(String objectType, String title, int line, int column) implements ArgumentNode {}
}
