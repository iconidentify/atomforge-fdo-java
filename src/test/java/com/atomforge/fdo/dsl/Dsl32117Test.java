package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify 32-117 (chat room UI) compiles correctly to match Ada32.
 *
 * 32-117 is a complex chat room UI with:
 * - Button bar (List Rooms, Center Stage, PC Studio, Preferences)
 * - People list (dss_list)
 * - Chat view
 * - Message input with Send button
 * - Complex nested action streams
 *
 * The authoritative test is text compilation matching Ada32.
 * For DSL code generation, see DslCodeGenerator which can convert
 * the FDO source to Java DSL code that produces byte-identical output.
 */
@DisplayName("32-117 Chat Room UI Compilation")
class Dsl32117Test {

    /**
     * Verify that the FDO text compiler produces byte-identical output to Ada32.
     * This is the authoritative test - it proves the core compiler is correct.
     */
    @Test
    @DisplayName("32-117: Text compiler matches Ada32")
    void test32_117_TextCompilerMatchesAda32() throws Exception {
        // Load original source
        String source;
        try (InputStream is = getClass().getResourceAsStream("/golden_ada32/32-117.fdo.source.txt")) {
            assertNotNull(is, "32-117.fdo.source.txt not found");
            source = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        // Compile using FdoCompiler
        byte[] textCompiled = FdoCompiler.create().compile(source);

        // Load Ada32 reference binary
        byte[] ada32Binary;
        try (InputStream is = getClass().getResourceAsStream("/golden_ada32/32-117.fdo.compiled.bin")) {
            assertNotNull(is, "32-117.fdo.compiled.bin not found");
            ada32Binary = is.readAllBytes();
        }

        // Compare - this must match exactly
        assertArrayEquals(ada32Binary, textCompiled, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("32-117 text compiler output does not match Ada32\n");
            sb.append("Ada32 size: ").append(ada32Binary.length).append(" bytes\n");
            sb.append("Text size: ").append(textCompiled.length).append(" bytes\n");

            int minLen = Math.min(ada32Binary.length, textCompiled.length);
            for (int i = 0; i < minLen; i++) {
                if (ada32Binary[i] != textCompiled[i]) {
                    sb.append("\nFirst diff at offset ").append(i)
                        .append(" (0x").append(Integer.toHexString(i)).append(")\n");
                    sb.append("  Ada32: 0x").append(String.format("%02X", ada32Binary[i] & 0xFF)).append("\n");
                    sb.append("  Text:  0x").append(String.format("%02X", textCompiled[i] & 0xFF)).append("\n");
                    break;
                }
            }
            return sb.toString();
        });
    }
}
