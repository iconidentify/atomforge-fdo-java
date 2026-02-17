package com.atomforge.fdo;

import org.junit.jupiter.api.Test;

class UniStartStreamArgTest {
    @Test
    void testNestedStreamWithArg() throws Exception {
        String source = """
            uni_start_stream
            act_replace_select_action
                <
                uni_start_stream <00x>
                  man_close_update
                uni_end_stream
                >
            uni_end_stream
            """;

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        // Ada32 produces: 0001 0002 040a 0001 0100 011c 0000 0200 0002 00 (19 bytes)
        System.out.println("Expected length: 19");
        System.out.println("Actual length: " + actual.length);
        System.out.println("Actual: " + toHex(actual));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
