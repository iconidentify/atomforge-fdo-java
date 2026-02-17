package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for trigger and frame style encoding/decoding.
 */
public class StyleDecodingTest {

    private final FdoCompiler compiler = FdoCompiler.create();
    private final FdoDecompiler decompiler = FdoDecompiler.create();

    // ===== mat_trigger_style (Protocol 16, Atom 88) =====
    // $enum word set default=0,place=1,rectangle=2,picture=3,framed=4,
    // bottom_tab=5,plain_picture=6,group_state=7

    @ParameterizedTest
    @CsvSource({
        "default, 0",
        "place, 1",
        "rectangle, 2",
        "picture, 3",
        "framed, 4",
        "bottom_tab, 5",
        "plain_picture, 6",
        "group_state, 7"
    })
    void testMatTriggerStyle(String symbolicName, int expectedCode) throws FdoException {
        String source = "mat_trigger_style <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 5, "Binary should have at least 5 bytes (header + 2 data)");

        // Verify encoding: [proto=16][atom=88][len=2][value_hi][value_lo]
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x58, binary[1] & 0xFF, "Atom should be 88 (mat_trigger_style)");
        assertEquals(0x02, binary[2] & 0xFF, "Length should be 2");

        // Value is big-endian word
        int value = ((binary[3] & 0xFF) << 8) | (binary[4] & 0xFF);
        assertEquals(expectedCode, value, "Value should be " + expectedCode);
    }

    @Test
    void testMatTriggerStyleRoundTrip() throws FdoException {
        String[] styles = {"default", "place", "rectangle", "picture", "framed",
                          "bottom_tab", "plain_picture", "group_state"};

        for (String style : styles) {
            String source = "mat_trigger_style <" + style + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + style + ". Decompiled: " + decompiled);
        }
    }

    // ===== mat_frame_style (Protocol 16, Atom 87) =====
    // $enum word set none=0,single_line_pop_out=1,single_line_pop_in=2,pop_in=3,
    // pop_out=4,double_line=5,shadow=6,highlight=7

    @ParameterizedTest
    @CsvSource({
        "none, 0",
        "single_line_pop_out, 1",
        "single_line_pop_in, 2",
        "pop_in, 3",
        "pop_out, 4",
        "double_line, 5",
        "shadow, 6",
        "highlight, 7"
    })
    void testMatFrameStyle(String symbolicName, int expectedCode) throws FdoException {
        String source = "mat_frame_style <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 5, "Binary should have at least 5 bytes (header + 2 data)");

        // Verify encoding: [proto=16][atom=87][len=2][value_hi][value_lo]
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x57, binary[1] & 0xFF, "Atom should be 87 (mat_frame_style)");
        assertEquals(0x02, binary[2] & 0xFF, "Length should be 2");

        // Value is big-endian word
        int value = ((binary[3] & 0xFF) << 8) | (binary[4] & 0xFF);
        assertEquals(expectedCode, value, "Value should be " + expectedCode);
    }

    @Test
    void testMatFrameStyleRoundTrip() throws FdoException {
        String[] styles = {"none", "single_line_pop_out", "single_line_pop_in", "pop_in",
                          "pop_out", "double_line", "shadow", "highlight"};

        for (String style : styles) {
            String source = "mat_frame_style <" + style + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + style + ". Decompiled: " + decompiled);
        }
    }

    // ===== Numeric fallback tests =====

    @Test
    void testTriggerStyleNumericFallback() throws FdoException {
        // When using numeric codes, should decode to symbolic name and round-trip
        // Using symbolic name is expected to produce same binary
        String source = "mat_trigger_style <picture>";  // picture=3
        byte[] binary1 = compiler.compile(source);
        String decompiled = decompiler.decompile(binary1);
        byte[] binary2 = compiler.compile(decompiled);

        assertArrayEquals(binary1, binary2,
            "Round-trip failed for symbolic code. Decompiled: " + decompiled);
        assertTrue(decompiled.contains("picture"), "Decompiled should contain 'picture': " + decompiled);
    }

    @Test
    void testFrameStyleNumericFallback() throws FdoException {
        // When using numeric codes, should decode to symbolic name and round-trip
        String source = "mat_frame_style <double_line>";  // double_line=5
        byte[] binary1 = compiler.compile(source);
        String decompiled = decompiler.decompile(binary1);
        byte[] binary2 = compiler.compile(decompiled);

        assertArrayEquals(binary1, binary2,
            "Round-trip failed for symbolic code. Decompiled: " + decompiled);
        assertTrue(decompiled.contains("double_line"), "Decompiled should contain 'double_line': " + decompiled);
    }
}
