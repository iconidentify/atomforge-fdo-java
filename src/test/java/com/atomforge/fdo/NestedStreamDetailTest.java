package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import java.io.*;

class NestedStreamDetailTest {
    @Test
    void testFirstNestedAction() throws Exception {
        // The first act_replace_select_action from test_1082 (lines 161-177)
        String source = """
            uni_start_stream
            act_replace_select_action
                <
                uni_start_stream
                  fm_start
                  fm_item_type <filename>
                  fm_item_set <"d">
                  fm_add_file_type_mask
                  fm_dialog_put_file <"Main Gamma Item">
                  fm_handle_error <display_msg | terminate>
                  fm_item_get <42>
                  uni_save_result
                  man_set_context_relative <9>
                  uni_get_result
                  uni_use_last_atom_string <man_replace_data>
                  man_update_display
                uni_end_stream
                >
            uni_end_stream
            """;

        FdoCompiler compiler = FdoCompiler.create();
        byte[] actual = compiler.compile(source);

        System.out.println("Our compilation:");
        System.out.println("  Total length: " + actual.length);
        System.out.println("  Hex: " + toHex(actual));

        // Use Ada32 API to get expected
        String json = "{\"source\": \"" + source.replace("\n", "\\n").replace("\"", "\\\"") + "\"}";
        System.out.println("\nComparing with Ada32...");

        // For now, just show the structure
        analyzeBytes(actual);
    }

    private void analyzeBytes(byte[] data) {
        System.out.println("\nByte analysis:");
        int pos = 0;
        while (pos < data.length) {
            int firstByte = data[pos] & 0xFF;
            int style = (firstByte >> 5) & 0x07;
            int proto = firstByte & 0x1F;

            if (style == 0) { // FULL style
                if (pos + 1 >= data.length) break;
                int atom = data[pos + 1] & 0xFF;
                if (pos + 2 >= data.length) break;
                int lenByte = data[pos + 2] & 0xFF;
                int length;
                int headerLen;
                if ((lenByte & 0x80) != 0) {
                    // 2-byte length
                    if (pos + 3 >= data.length) break;
                    length = ((lenByte & 0x7F) << 8) | (data[pos + 3] & 0xFF);
                    headerLen = 4;
                } else {
                    length = lenByte;
                    headerLen = 3;
                }

                String name = getAtomName(proto, atom);
                System.out.printf("  @%d: proto=%d atom=%d (%s) len=%d%n",
                    pos, proto, atom, name, length);

                if (name.contains("action") && length > 10) {
                    // Show nested stream content
                    System.out.println("       Nested stream data:");
                    int dataStart = pos + headerLen;
                    int dataEnd = dataStart + Math.min(length, 50);
                    System.out.print("       ");
                    for (int i = dataStart; i < dataEnd && i < data.length; i++) {
                        System.out.printf("%02x ", data[i] & 0xFF);
                    }
                    if (length > 50) System.out.print("...");
                    System.out.println();
                }

                pos += headerLen + length;
            } else {
                // Skip other styles for now
                pos++;
            }
        }
    }

    private String getAtomName(int proto, int atom) {
        if (proto == 0) {
            return switch(atom) {
                case 1 -> "uni_start_stream";
                case 2 -> "uni_end_stream";
                default -> "uni_" + atom;
            };
        } else if (proto == 2) {
            return switch(atom) {
                case 4 -> "act_replace_select_action";
                default -> "act_" + atom;
            };
        }
        return "proto" + proto + "_atom" + atom;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 32 == 0) sb.append("\n        ");
            else if (i > 0) sb.append(" ");
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
