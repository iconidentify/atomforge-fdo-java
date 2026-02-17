package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for mat_font_sis composite atom parsing.
 *
 * mat_font_sis format: [font_id, size, style]
 * - Byte 1: Font ID enum (arial=0, courier=1, times_roman=2, etc.)
 * - Byte 2: Font size (number)
 * - Byte 3: Style flags (normal=0, bold=1, italic=2, underline=4 - OR'd together)
 */
public class FontSisTest {

    private final FdoCompiler compiler = FdoCompiler.create();
    private final FdoDecompiler decompiler = FdoDecompiler.create();

    // ===== Font ID Tests =====

    @ParameterizedTest
    @CsvSource({
        "arial, 0",
        "courier, 1",
        "times_roman, 2",
        "system, 3",
        "fixed_system, 4",
        "ms_serif, 5",
        "ms_sans_serif, 6",
        "small_fonts, 7",
        "courier_new, 8"
    })
    void testFontIds(String fontName, int expectedCode) throws FdoException {
        String source = "mat_font_sis <" + fontName + ", 10, normal>";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        String decompiled = decompiler.decompile(binary);
        assertTrue(decompiled.contains("mat_font_sis"),
            "Decompiled should contain mat_font_sis: " + decompiled);
    }

    // ===== Font Style Tests =====

    @ParameterizedTest
    @CsvSource({
        "normal, 0",
        "bold, 1",
        "italic, 2",
        "underline, 4"
    })
    void testFontStyles(String styleName, int expectedCode) throws FdoException {
        String source = "mat_font_sis <arial, 12, " + styleName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        String decompiled = decompiler.decompile(binary);
        assertTrue(decompiled.contains("mat_font_sis"),
            "Decompiled should contain mat_font_sis: " + decompiled);
    }

    // ===== Font Size Tests =====

    @ParameterizedTest
    @CsvSource({
        "8",
        "9",
        "10",
        "12",
        "14",
        "18",
        "24"
    })
    void testFontSizes(int size) throws FdoException {
        String source = "mat_font_sis <arial, " + size + ", normal>";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        String decompiled = decompiler.decompile(binary);
        assertTrue(decompiled.contains("mat_font_sis"),
            "Decompiled should contain mat_font_sis: " + decompiled);
    }

    // ===== Round-trip Tests =====

    @Test
    void testRoundTripBasic() throws FdoException {
        String source = "mat_font_sis <arial, 10, normal>";
        byte[] binary1 = compiler.compile(source);
        String decompiled = decompiler.decompile(binary1);
        byte[] binary2 = compiler.compile(decompiled);

        assertArrayEquals(binary1, binary2,
            "Round-trip failed. Decompiled: " + decompiled);
    }

    @Test
    void testRoundTripBold() throws FdoException {
        String source = "mat_font_sis <courier, 12, bold>";
        byte[] binary1 = compiler.compile(source);
        String decompiled = decompiler.decompile(binary1);
        byte[] binary2 = compiler.compile(decompiled);

        assertArrayEquals(binary1, binary2,
            "Round-trip failed. Decompiled: " + decompiled);
    }

    @Test
    void testRoundTripVariousFonts() throws FdoException {
        String[] fonts = {"arial", "courier", "times_roman", "system", "small_fonts"};
        String[] styles = {"normal", "bold", "italic"};
        int[] sizes = {8, 10, 12};

        for (String font : fonts) {
            for (String style : styles) {
                for (int size : sizes) {
                    String source = String.format("mat_font_sis <%s, %d, %s>", font, size, style);
                    byte[] binary1 = compiler.compile(source);
                    String decompiled = decompiler.decompile(binary1);
                    byte[] binary2 = compiler.compile(decompiled);

                    assertArrayEquals(binary1, binary2,
                        "Round-trip failed for " + source + ". Decompiled: " + decompiled);
                }
            }
        }
    }

    // ===== Binary Format Verification =====

    @Test
    void testBinaryFormat() throws FdoException {
        // mat_font_sis <arial, 10, normal> should encode as:
        // [proto=16][atom=10][len=3][font=0][size=10][style=0]
        String source = "mat_font_sis <arial, 10, normal>";
        byte[] binary = compiler.compile(source);

        // Verify format
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x0A, binary[1] & 0xFF, "Atom should be 10 (mat_font_sis)");
        assertEquals(0x03, binary[2] & 0xFF, "Length should be 3");
        assertEquals(0x00, binary[3] & 0xFF, "Font ID should be 0 (arial)");
        assertEquals(0x0A, binary[4] & 0xFF, "Size should be 10");
        assertEquals(0x00, binary[5] & 0xFF, "Style should be 0 (normal)");
    }

    @Test
    void testBinaryFormatWithStyle() throws FdoException {
        // mat_font_sis <courier, 12, bold> should encode as:
        // [proto=16][atom=10][len=3][font=1][size=12][style=1]
        String source = "mat_font_sis <courier, 12, bold>";
        byte[] binary = compiler.compile(source);

        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x0A, binary[1] & 0xFF, "Atom should be 10 (mat_font_sis)");
        assertEquals(0x03, binary[2] & 0xFF, "Length should be 3");
        assertEquals(0x01, binary[3] & 0xFF, "Font ID should be 1 (courier)");
        assertEquals(0x0C, binary[4] & 0xFF, "Size should be 12");
        assertEquals(0x01, binary[5] & 0xFF, "Style should be 1 (bold)");
    }

    // ===== Piped Style Tests (Future) =====

    // TODO: Test piped styles like <arial, 10, bold | italic>
    // This would require style code = 1 | 2 = 3
}
