package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for rich enum symbolic names.
 */
public class EnumSymbolicNamesTest {

    private final FdoCompiler compiler = FdoCompiler.create();
    private final FdoDecompiler decompiler = FdoDecompiler.create();

    // ===== de_set_data_type (Protocol 3, Atom 3) =====
    // $enum byte set default=0,text=1,var=2,boolean=3,global_id=4,relative_id=5,
    // index=6,child_count=7,objptr=8,value=9,raw=10,length=11,selected=12

    @ParameterizedTest
    @CsvSource({
        "default, 0",
        "text, 1",
        "var, 2",
        "boolean, 3",
        "global_id, 4",
        "relative_id, 5",
        "index, 6",
        "child_count, 7",
        "objptr, 8",
        "value, 9",
        "raw, 10",
        "length, 11",
        "selected, 12"
    })
    void testDeSetDataType(String symbolicName, int expectedCode) throws FdoException {
        String source = "de_set_data_type <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 4, "Binary should have at least 4 bytes");

        // Verify encoding: [proto=3][atom=3][len=1][value]
        assertEquals(0x03, binary[0] & 0xFF, "Protocol should be 3 (DE)");
        assertEquals(0x03, binary[1] & 0xFF, "Atom should be 3 (de_set_data_type)");
        assertEquals(0x01, binary[2] & 0xFF, "Length should be 1");
        assertEquals(expectedCode, binary[3] & 0xFF, "Value should be " + expectedCode);
    }

    @Test
    void testDeSetDataTypeRoundTrip() throws FdoException {
        String[] types = {"default", "text", "var", "boolean", "global_id", "relative_id",
                         "index", "child_count", "objptr", "value", "raw", "length", "selected"};

        for (String type : types) {
            String source = "de_set_data_type <" + type + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + type + ". Decompiled: " + decompiled);
        }
    }

    // ===== mat_log_object (Protocol 16, Atom 65) =====
    // $enum byte set session_log=0,chat_log=1,im_log=2,no_log=3

    @ParameterizedTest
    @CsvSource({
        "session_log, 0",
        "chat_log, 1",
        "im_log, 2",
        "no_log, 3"
    })
    void testMatLogObject(String symbolicName, int expectedCode) throws FdoException {
        String source = "mat_log_object <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 4, "Binary should have at least 4 bytes");

        // Verify encoding: [proto=16][atom=65][len=1][value]
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x41, binary[1] & 0xFF, "Atom should be 65 (mat_log_object)");
        assertEquals(0x01, binary[2] & 0xFF, "Length should be 1");
        assertEquals(expectedCode, binary[3] & 0xFF, "Value should be " + expectedCode);
    }

    @Test
    void testMatLogObjectRoundTrip() throws FdoException {
        String[] types = {"session_log", "chat_log", "im_log", "no_log"};

        for (String type : types) {
            String source = "mat_log_object <" + type + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + type + ". Decompiled: " + decompiled);
        }
    }

    // ===== mat_sort_order (Protocol 16, Atom 69) =====
    // $enum byte set normal=0,reverse=1,alphabetical=2

    @ParameterizedTest
    @CsvSource({
        "normal, 0",
        "reverse, 1",
        "alphabetical, 2"
    })
    void testMatSortOrder(String symbolicName, int expectedCode) throws FdoException {
        String source = "mat_sort_order <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 4, "Binary should have at least 4 bytes");

        // Verify encoding: [proto=16][atom=69][len=1][value]
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x45, binary[1] & 0xFF, "Atom should be 69 (mat_sort_order)");
        assertEquals(0x01, binary[2] & 0xFF, "Length should be 1");
        assertEquals(expectedCode, binary[3] & 0xFF, "Value should be " + expectedCode);
    }

    @Test
    void testMatSortOrderRoundTrip() throws FdoException {
        String[] types = {"normal", "reverse", "alphabetical"};

        for (String type : types) {
            String source = "mat_sort_order <" + type + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + type + ". Decompiled: " + decompiled);
        }
    }

    // ===== mat_position (Protocol 16, Atom 64) =====
    // $enum byte set cascade=0,top_left=1,top_center=2,top_right=3,center_left=4,
    // center_center=5,center_right=6,bottom_left=7,bottom_center=8,bottom_right=9

    @ParameterizedTest
    @CsvSource({
        "cascade, 0",
        "top_left, 1",
        "top_center, 2",
        "top_right, 3",
        "center_left, 4",
        "center_center, 5",
        "center_right, 6",
        "bottom_left, 7",
        "bottom_center, 8",
        "bottom_right, 9"
    })
    void testMatPosition(String symbolicName, int expectedCode) throws FdoException {
        String source = "mat_position <" + symbolicName + ">";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length >= 4, "Binary should have at least 4 bytes");

        // Verify encoding: [proto=16][atom=64][len=1][value]
        assertEquals(0x10, binary[0] & 0xFF, "Protocol should be 16 (MAT)");
        assertEquals(0x40, binary[1] & 0xFF, "Atom should be 64 (mat_position)");
        assertEquals(0x01, binary[2] & 0xFF, "Length should be 1");
        assertEquals(expectedCode, binary[3] & 0xFF, "Value should be " + expectedCode);
    }

    @Test
    void testMatPositionRoundTrip() throws FdoException {
        String[] positions = {"cascade", "top_left", "top_center", "top_right", "center_left",
                             "center_center", "center_right", "bottom_left", "bottom_center", "bottom_right"};

        for (String pos : positions) {
            String source = "mat_position <" + pos + ">";
            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + pos + ". Decompiled: " + decompiled);
        }
    }

    // ===== Numeric fallback tests =====

    @Test
    void testNumericFallback() throws FdoException {
        // When using numeric codes, should work and round-trip
        String source = "de_set_data_type <5>";
        byte[] binary1 = compiler.compile(source);
        String decompiled = decompiler.decompile(binary1);
        byte[] binary2 = compiler.compile(decompiled);

        assertArrayEquals(binary1, binary2,
            "Round-trip failed for numeric code. Decompiled: " + decompiled);
    }
}
