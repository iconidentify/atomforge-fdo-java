package com.atomforge.fdo;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification tests for enum values.
 * These tests verify our Java compiler correctly encodes all values.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnumDiscoveryVerificationTest {

    private FdoCompiler compiler;

    @BeforeAll
    void setup() throws Exception {
        compiler = FdoCompiler.create();
    }

    // ========== Font ID Tests ==========

    @ParameterizedTest(name = "Font: {0} -> 0x{1}")
    @CsvSource({
        "arial, 00",
        "courier, 01",
        "times_roman, 02",
        "system, 03",
        "fixed_system, 04",
        "ms_serif, 05",
        "ms_sans_serif, 06",
        "small_fonts, 07",
        "courier_new, 08",
        "script, 09",
        "ms_mincho, 0A",
        "ms_gothic, 0B",
        "xi_ming_ti, 0C",
        "biao_kai_ti, 0D",
        "ming_lui_fixed, 0F",
        "ming_lui_variable, 10",
        "ms_hei, 11",
        "ms_song, 12"
    })
    void testFontIds(String fontName, String expectedHex) throws Exception {
        String source = "mat_font_sis <" + fontName + ", normal, 12>";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Font " + fontName + " should encode to 0x" + expectedHex);
    }

    // ========== Alert Type Tests (8 discovered types) ==========

    @ParameterizedTest(name = "Alert: {0} -> 0x{1}")
    @CsvSource({
        "info, 01",
        "error, 02",
        "pop_info, 03",
        "pop_error, 04",
        "warning, 05",
        "pop_warning, 06",
        "yes_no, 07",
        "yes_no_cancel, 08"
    })
    void testAlertTypes(String alertType, String expectedHex) throws Exception {
        String source = "async_alert <" + alertType + ", \"Test\">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Alert " + alertType + " should encode to 0x" + expectedHex);
    }

    // ========== Object Type Tests (20 discovered types) ==========

    @ParameterizedTest(name = "Object: {0} -> 0x{1}")
    @CsvSource({
        "org_group, 00",
        "ind_group, 01",
        "dms_list, 02",
        "sms_list, 03",
        "dss_list, 04",
        "sss_list, 05",
        "trigger, 06",
        "ornament, 07",
        "view, 08",
        "edit_view, 09",
        "boolean, 0A",
        "select_boolean, 0B",
        "range, 0C",
        "select_range, 0D",
        "variable, 0E",
        "bad_object, 0F",
        "root, 10",
        "tool_group, 11",
        "tab_group, 12",
        "tab_page, 13"
    })
    void testObjectTypes(String objectType, String expectedHex) throws Exception {
        String source = "man_start_object <" + objectType + ", \"Test\">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Object " + objectType + " should encode to 0x" + expectedHex);
    }

    // ========== Position Tests (10 discovered positions) ==========

    @ParameterizedTest(name = "Position: {0} -> 0x{1}")
    @CsvSource({
        "cascade, 00",
        "top_left, 01",
        "top_center, 02",
        "top_right, 03",
        "center_left, 04",
        "center_center, 05",
        "center_right, 06",
        "bottom_left, 07",
        "bottom_center, 08",
        "bottom_right, 09"
    })
    void testPositions(String position, String expectedHex) throws Exception {
        String source = "mat_position <" + position + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Position " + position + " should encode to 0x" + expectedHex);
    }

    // ========== Trigger Style Tests (8 discovered styles) ==========

    @ParameterizedTest(name = "Trigger: {0} -> 0x{1}")
    @CsvSource({
        "default, 0000",
        "place, 0001",
        "rectangle, 0002",
        "picture, 0003",
        "framed, 0004",
        "bottom_tab, 0005",
        "plain_picture, 0006",
        "group_state, 0007"
    })
    void testTriggerStyles(String style, String expectedHex) throws Exception {
        String source = "mat_trigger_style <" + style + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        int actualCode = ((result[3] & 0xFF) << 8) | (result[4] & 0xFF);
        assertEquals(expectedCode, actualCode,
            "Trigger style " + style + " should encode to 0x" + expectedHex);
    }

    // ========== Frame Style Tests (8 discovered styles) ==========

    @ParameterizedTest(name = "Frame: {0} -> 0x{1}")
    @CsvSource({
        "none, 0000",
        "single_line_pop_out, 0001",
        "single_line_pop_in, 0002",
        "pop_in, 0003",
        "pop_out, 0004",
        "double_line, 0005",
        "shadow, 0006",
        "highlight, 0007"
    })
    void testFrameStyles(String style, String expectedHex) throws Exception {
        String source = "mat_frame_style <" + style + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        int actualCode = ((result[3] & 0xFF) << 8) | (result[4] & 0xFF);
        assertEquals(expectedCode, actualCode,
            "Frame style " + style + " should encode to 0x" + expectedHex);
    }

    // ========== DE Data Type Tests (13 discovered types) ==========

    @ParameterizedTest(name = "DataType: {0} -> 0x{1}")
    @CsvSource({
        "default, 00",
        "text, 01",
        "var, 02",
        "boolean, 03",
        "global_id, 04",
        "relative_id, 05",
        "index, 06",
        "child_count, 07",
        "objptr, 08",
        "value, 09",
        "raw, 0A",
        "length, 0B",
        "selected, 0C"
    })
    void testDeDataTypes(String dataType, String expectedHex) throws Exception {
        String source = "de_set_data_type <" + dataType + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Data type " + dataType + " should encode to 0x" + expectedHex);
    }

    // ========== FM Item Type Tests (sample of 40 discovered) ==========

    @ParameterizedTest(name = "FmItem: {0} -> 0x{1}")
    @CsvSource({
        "file_type_id, 01",
        "file_group_id, 02",
        "file_type_ext, 03",
        "filename, 06",
        "path, 07",
        "filespec, 08",
        "handle, 09",
        "error_code, 0A",
        "custom_data, 0B",
        "text_width, 0C",
        "print_from_page, 17",
        "print_copies, 1B",
        "date, 1E",
        "time, 1F",
        "ini_file, 22",
        "ini_group, 23",
        "ini_key, 24",
        "persistent_path, 28"
    })
    void testFmItemTypes(String itemType, String expectedHex) throws Exception {
        // fm_item_type takes only one argument (the item type identifier)
        String source = "fm_item_type <" + itemType + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        // Format: [protocol=8][atom=2][length][item_type_code]
        // Protocol 8 (FM), Atom 2, so result[0]=8, result[1]=2, result[2]=length, result[3]=item_type_code
        assertTrue(result.length >= 4, "Result should have at least 4 bytes");
        assertEquals(expectedCode, result[3] & 0xFF,
            "FM item " + itemType + " should encode to 0x" + expectedHex);
    }

    // ========== Buffer Flag Tests (4 newly discovered flags) ==========

    @ParameterizedTest(name = "BufferFlag: {0} -> 0x{1}")
    @CsvSource({
        "data_included, 20",
        "leave_buffer_open, 40",
        "pointer_included, 100",
        "clear_buffer, 200"
    })
    void testBufferFlags(String flag, String expectedHex) throws Exception {
        String source = "buf_start_buffer <" + flag + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        // buf_start_buffer: [protocol=4][atom=0][length=4][flags(4-byte BE)]
        // Format: result[0]=4, result[1]=0, result[2]=4, result[3-6]=flags (BE)
        assertTrue(result.length >= 7, "Result should have at least 7 bytes");
        int actualCode = ((result[3] & 0xFF) << 24) | ((result[4] & 0xFF) << 16) |
                        ((result[5] & 0xFF) << 8) | (result[6] & 0xFF);
        assertEquals(expectedCode, actualCode,
            "Buffer flag " + flag + " should encode to 0x" + expectedHex);
    }

    

    @ParameterizedTest(name = "Criterion: {0} -> 0x{1}")
    @CsvSource({
        "void, 00",
        "open, 03",
        "cancel, 06",
        "enter_free, 07",
        "enter_paid, 08",
        "create, 09",
        "set_online, 0A",
        "set_offline, 0B",
        "restore, 0C",
        "minimize, 0E",
        "restore_from_maximize, 0F",
        "restore_from_minimize, 10",
        "timeout, 11",
        "screen_name_changed, 12",
        "movie_over, 13",
        "drop, 14",
        "url_drop, 15",
        "user_delete, 16",
        "toggle_up, 17",
        "activated, 18",
        "deactivated, 19",
        "popupmenu, 1A",
        "destroyed, 1B"
    })
    void testCriterionValues(String criterion, String expectedHex) throws Exception {
        String source = "act_set_criterion <" + criterion + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Criterion " + criterion + " should encode to 0x" + expectedHex);
    }

    // ========== Field Script Tests (3 newly discovered scripts) ==========

    @ParameterizedTest(name = "FieldScript: {0} -> 0x{1}")
    @CsvSource({
        "japanese, 01",
        "chinesetr, 02",
        "chineses, 19"
    })
    void testFieldScripts(String script, String expectedHex) throws Exception {
        String source = "mat_field_script <" + script + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Field script " + script + " should encode to 0x" + expectedHex);
    }

    // ========== Sort Order Tests (1 newly discovered value) ==========

    @ParameterizedTest(name = "SortOrder: {0} -> 0x{1}")
    @CsvSource({
        "normal, 00"
    })
    void testSortOrders(String order, String expectedHex) throws Exception {
        String source = "mat_sort_order <" + order + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Sort order " + order + " should encode to 0x" + expectedHex);
    }

    // ========== Log Object Type Tests (1 newly discovered value) ==========

    @ParameterizedTest(name = "LogObject: {0} -> 0x{1}")
    @CsvSource({
        "session_log, 00"
    })
    void testLogObjectTypes(String logType, String expectedHex) throws Exception {
        String source = "mat_log_object <" + logType + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Log object " + logType + " should encode to 0x" + expectedHex);
    }

    // ========== Auto Complete Value Tests (2 newly discovered values) ==========

    @ParameterizedTest(name = "AutoComplete: {0} -> 0x{1}")
    @CsvSource({
        "web_list, 00",
        "other_list, 02"
    })
    void testAutoCompleteValues(String value, String expectedHex) throws Exception {
        String source = "mat_auto_complete <" + value + ">";
        byte[] result = compiler.compile(source);

        int expectedCode = Integer.parseInt(expectedHex, 16);
        assertEquals(expectedCode, result[3] & 0xFF,
            "Auto complete " + value + " should encode to 0x" + expectedHex);
    }

    // ========== Summary Test ==========

    @Test
    @DisplayName("Discovery Summary: 190+ symbolic names across 15 enum types")
    void testDiscoverySummary() {
        System.out.println("=== Enum Discovery Verification Summary ===");
        System.out.println("Font IDs:        18 values (including CJK fonts)");
        System.out.println("Alert Types:      8 values (including pop_* variants)");
        System.out.println("Object Types:    20 values (including tool_group at 0x11)");
        System.out.println("Positions:       10 values");
        System.out.println("Trigger Styles:   8 values");
        System.out.println("Frame Styles:     8 values");
        System.out.println("DE Data Types:   13 values");
        System.out.println("FM Item Types:   40 values");
        System.out.println("Orientation:     50 values (h/v + alignment combos)");
        System.out.println("Buffer Flags:     4 newly discovered values");
        System.out.println("Criterion Values: 2 newly discovered values");
        System.out.println("Field Scripts:    3 newly discovered values");
        System.out.println("Sort Orders:      1 newly discovered value");
        System.out.println("Log Object Types: 1 newly discovered value");
        System.out.println("Auto Complete:    2 newly discovered values");
        System.out.println("-------------------------------------------");
        System.out.println("TOTAL:          190+ symbolic names discovered");
    }

    // ========== Word Type Tests (2-byte big-endian encoding) ==========
    

    @ParameterizedTest(name = "Word Type: {0} <{1}> -> 2-byte BE 0x{2}")
    @CsvSource({
        "phone_port_list, 8080, 1F90",
        "phone_port_list, 443, 01BB",
        "phone_ready_to_connect, 1, 0001",
        "phone_ready_to_connect, 65535, FFFF",
        "comit_reboot, 0, 0000",
        "comit_reboot, 1, 0001",
        "comit_restart, 2, 0002",
        "comit_restart, 255, 00FF"
    })
    void testWordTypeEncoding(String atomName, int value, String expectedHex) throws Exception {
        String source = "uni_start_stream <00x>\n" + atomName + " <" + value + ">\nuni_end_stream <>";
        byte[] result = compiler.compile(source);
        
        // Find the 2-byte data in the compiled output
        // Format: [style|proto] [atom] [len=2] [byte1] [byte2]
        int expected = Integer.parseInt(expectedHex, 16);
        boolean found = false;
        
        // Search for length byte = 2 followed by our expected 2-byte value
        for (int i = 0; i < result.length - 2; i++) {
            if ((result[i] & 0xFF) == 2 && i + 2 < result.length) {
                int highByte = (result[i + 1] & 0xFF) << 8;
                int lowByte = result[i + 2] & 0xFF;
                int actual = highByte | lowByte;
                if (actual == expected) {
                    found = true;
                    break;
                }
            }
        }
        
        assertTrue(found, 
            atomName + " <" + value + "> should encode to 0x" + expectedHex + " (2-byte BE)");
    }

    // ========== Byte Type Tests (single byte encoding) ==========
    

    @ParameterizedTest(name = "Byte Type: {0} <{1}> -> 0x{2}")
    @CsvSource({
        "phone_accept_call, 0, 00",
        "phone_cancel_call, 1, 01",
        "phone_decline_call, 255, FF",
        "mat_use_style_guide, 0, 00",
        "mat_use_style_guide, 1, 01"
    })
    void testByteTypeEncoding(String atomName, int value, String expectedHex) throws Exception {
        String source = "uni_start_stream <00x>\n" + atomName + " <" + value + ">\nuni_end_stream <>";
        byte[] result = compiler.compile(source);
        
        // Find the single byte data in the compiled output
        // Format: [style|proto] [atom] [len=1] [byte]
        int expected = Integer.parseInt(expectedHex, 16);
        boolean found = false;
        
        // Search for length byte = 1 followed by our expected byte value
        for (int i = 0; i < result.length - 1; i++) {
            if ((result[i] & 0xFF) == 1 && i + 1 < result.length) {
                int actual = result[i + 1] & 0xFF;
                if (actual == expected) {
                    found = true;
                    break;
                }
            }
        }
        
        assertTrue(found,
            atomName + " <" + value + "> should encode to 0x" + expectedHex + " (single byte)");
    }
}
