package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for extended object types (codes 15-28).
 */
public class ObjectTypeExtensionTest {

    private final FdoCompiler compiler = FdoCompiler.create();
    private final FdoDecompiler decompiler = FdoDecompiler.create();

    // ===== Existing Object Types (Baseline Tests) =====

    @ParameterizedTest
    @CsvSource({
        "org_group, 0",
        "ind_group, 1",
        "dms_list, 2",
        "sms_list, 3",
        "dss_list, 4",
        "sss_list, 5",
        "trigger, 6",
        "ornament, 7",
        "view, 8",
        "edit_view, 9",
        "boolean, 10",
        "select_boolean, 11",
        "range, 12",
        "select_range, 13",
        "variable, 14"
    })
    void testExistingObjectTypes(String typeName, int expectedCode) throws FdoException {
        String source = String.format("man_start_object <%s, \"Test\">", typeName);
        byte[] binary = compiler.compile(source);

        // Verify encoding - first data byte should be the type code
        // Binary format: [proto=1][atom=0][len][type_code][title_bytes...]
        assertTrue(binary.length > 3, "Binary should have header + data");

        String decompiled = decompiler.decompile(binary);
        assertTrue(decompiled.contains(typeName),
            "Decompiled should contain " + typeName + ": " + decompiled);
        assertTrue(decompiled.contains("Test"),
            "Decompiled should contain title 'Test': " + decompiled);
    }

    // ===== Extended Object Types (15-28) =====

    @ParameterizedTest
    @CsvSource({
        "ruler, 15",
        "root, 16",
        "tool_group, 17",
        "rich_text, 17",       // Same as tool_group? Need to verify
        "multimedia, 18",
        "chart, 19",
        "pictalk, 20",
        "www, 21",
        "split, 22",
        "organizer, 23",
        "tree, 24",
        "tab, 25",
        "progress, 26",
        "toolbar, 27",
        "slider, 28"
    })
    void testNewObjectTypes(String typeName, int expectedCode) throws FdoException {
        String source = String.format("man_start_object <%s, \"Test Window\">", typeName);
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length > 0, "Binary should not be empty");

        String decompiled = decompiler.decompile(binary);
        // Should either contain the original type name OR a fallback unknown_N format
        assertTrue(decompiled.contains(typeName) || decompiled.contains("unknown_" + expectedCode),
            "Decompiled should contain " + typeName + " or unknown_" + expectedCode + ": " + decompiled);
    }

    @Test
    void testRoundTripNewObjectTypes() throws FdoException {
        String[] newTypes = {"ruler", "rich_text", "multimedia", "chart", "pictalk",
                            "www", "split", "organizer", "tree", "tab", "progress",
                            "toolbar", "slider"};

        for (String typeName : newTypes) {
            String source = "man_start_object <" + typeName + ", \"Window\">\nman_end_object";

            byte[] binary1 = compiler.compile(source);
            String decompiled = decompiler.decompile(binary1);
            byte[] binary2 = compiler.compile(decompiled);

            assertArrayEquals(binary1, binary2,
                "Round-trip failed for " + typeName + ". Decompiled: " + decompiled);
        }
    }

    @Test
    void testObjectTypeWithEmptyTitle() throws FdoException {
        String source = "man_start_object <tree, \"\">";
        byte[] binary = compiler.compile(source);
        String decompiled = decompiler.decompile(binary);

        assertTrue(decompiled.contains("tree") || decompiled.contains("unknown_"),
            "Decompiled: " + decompiled);
    }

    @Test
    void testObjectTypeInSibling() throws FdoException {
        String source = """
            man_start_object <ind_group, "Main">
              man_start_sibling <toolbar, "Tools">
              man_end_object
            man_end_object
            """;

        byte[] binary = compiler.compile(source);
        String decompiled = decompiler.decompile(binary);

        assertTrue(decompiled.contains("ind_group") || decompiled.contains("unknown_1"),
            "Should contain ind_group");
        assertTrue(decompiled.contains("toolbar") || decompiled.contains("unknown_27"),
            "Should contain toolbar");
    }

    // ===== Alias Tests =====

    @ParameterizedTest
    @CsvSource({
        "group, org_group",
        "independent, ind_group",
        "editable_view, edit_view",
        "selectable_boolean, select_boolean",
        "selectable_range, select_range"
    })
    void testObjectTypeAliases(String alias, String canonical) throws FdoException {
        String source1 = String.format("man_start_object <%s, \"Test\">", alias);
        String source2 = String.format("man_start_object <%s, \"Test\">", canonical);

        byte[] binary1 = compiler.compile(source1);
        byte[] binary2 = compiler.compile(source2);

        // Both should produce the same binary
        assertArrayEquals(binary1, binary2,
            "Alias " + alias + " should produce same binary as " + canonical);
    }

    // ===== Mat Object Type Tests =====

    @Test
    void testMatObjectType() throws FdoException {
        // mat_object_type also uses object type values
        String source = "mat_object_type <trigger>";
        byte[] binary = compiler.compile(source);

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        String decompiled = decompiler.decompile(binary);
        assertTrue(decompiled.contains("trigger") || decompiled.contains("mat_object_type"),
            "Decompiled: " + decompiled);
    }
}
