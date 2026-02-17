package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoDecompiler;
import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.atoms.*;
import com.atomforge.fdo.dsl.values.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FdoScript DSL API.
 */
class FdoScriptTest {

    @Test
    void testSimpleStream() throws FdoException {
        // Build a simple stream using the DSL
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.VIEW, "Hello")
                .data("Hello, World!")
            .endObject()
            .compile();

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        // Verify it can be decompiled
        String decompiled = FdoDecompiler.create().decompile(binary);
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("view"));
        assertTrue(decompiled.contains("Hello"));
        assertTrue(decompiled.contains("Hello, World!"));
    }

    @Test
    void testObjectWithAttributes() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.IND_GROUP, "Login Form")
                .orientation(Orientation.VCF)
                .position(Position.CENTER_CENTER)
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ind_group"));
        assertTrue(decompiled.contains("Login Form"));
    }

    @Test
    void testNestedObjects() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.IND_GROUP, "Parent")
                .startObject(ObjectType.VIEW, "Child1")
                    .data("First child")
                .endObject()
                .startObject(ObjectType.VIEW, "Child2")
                    .data("Second child")
                .endObject()
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ind_group"));
        assertTrue(decompiled.contains("Parent"));
        assertTrue(decompiled.contains("Child1"));
        assertTrue(decompiled.contains("Child2"));
    }

    @Test
    void testTriggerWithAction() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.TRIGGER, "OK Button")
                .triggerStyle(TriggerStyle.FRAMED)
                .onSelect(action -> action.data("Button clicked"))
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("trigger"));
        assertTrue(decompiled.contains("OK Button"));
    }

    @Test
    void testRawAtomAccess() throws FdoException {
        // Test using the lower-level atom methods
        byte[] binary = FdoScript.stream()
            .uni(UniAtom.START_STREAM)
            .man(ManAtom.START_OBJECT, ObjectType.VIEW, "Test")
            .de(DeAtom.DATA, "Test data")
            .man(ManAtom.END_OBJECT)
            .uni(UniAtom.END_STREAM)
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("view"));
        assertTrue(decompiled.contains("Test data"));
    }

    @Test
    void testBooleanAttributes() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.IND_GROUP, "Modal Window")
                .modal()
                .disabled()
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ind_group"));
    }

    @Test
    void testFontAttributes() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.VIEW, "Styled Text")
                .fontId(FontId.ARIAL)
                .fontSize(14)
                .fontStyle(FontStyle.BOLD)
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("view"));
        assertTrue(decompiled.contains("Styled Text"));
    }

    @Test
    void testFontSisCombo() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.VIEW, "Combo Font")
                .fontSis(FontId.COURIER, 12, FontStyle.ITALIC)
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("view"));
    }

    @Test
    void testSizeAttribute() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.EDIT_VIEW, "Input Field")
                .size(3, 40)
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("edit_view"));
    }

    @Test
    void testAllObjectTypes() throws FdoException {
        // Test that all object types compile correctly
        for (ObjectType type : ObjectType.values()) {
            byte[] binary = FdoScript.stream()
                .startObject(type, "Test " + type.name())
                .endObject()
                .compile();

            assertNotNull(binary, "Failed for type: " + type);
        }
    }

    @Test
    void testAllOrientations() throws FdoException {
        // Test common orientations
        Orientation[] orientations = {
            Orientation.VCF, Orientation.HEF, Orientation.HLF,
            Orientation.VCC, Orientation.HEC, Orientation.HLC
        };

        for (Orientation orient : orientations) {
            byte[] binary = FdoScript.stream()
                .startObject(ObjectType.IND_GROUP, "Test")
                    .orientation(orient)
                .endObject()
                .compile();

            assertNotNull(binary, "Failed for orientation: " + orient);
        }
    }

    @Test
    void testAllPositions() throws FdoException {
        for (Position pos : Position.values()) {
            byte[] binary = FdoScript.stream()
                .startObject(ObjectType.IND_GROUP, "Test")
                    .position(pos)
                .endObject()
                .compile();

            assertNotNull(binary, "Failed for position: " + pos);
        }
    }

    @Test
    void testParseMethod() throws FdoException {
        // Test the FdoScript.parse convenience method
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.VIEW, "Test")
                .data("Hello")
            .endObject()
            .compile();

        var stream = FdoScript.parse(binary);
        assertNotNull(stream);
    }

    @Test
    void testCompileTextMethod() throws FdoException {
        // Test the FdoScript.compile convenience method
        String source = "man_start_object <view, \"Test\">\n" +
                       "de_data <\"Hello\">\n" +
                       "man_end_object";

        byte[] binary = FdoScript.compile(source);
        assertNotNull(binary);
        assertTrue(binary.length > 0);
    }

    @Test
    void testDecompileMethod() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.VIEW, "Test")
                .data("Hello")
            .endObject()
            .compile();

        String source = FdoScript.decompile(binary);
        assertNotNull(source);
        assertTrue(source.contains("view"));
        assertTrue(source.contains("Hello"));
    }

    @Test
    void testSiblingObjects() throws FdoException {
        byte[] binary = FdoScript.stream()
            .startObject(ObjectType.IND_GROUP, "Container")
                .startObject(ObjectType.VIEW, "First")
                    .data("First view")
                .startSibling(ObjectType.VIEW, "Second")
                    .data("Second view")
                .endObject()
            .endObject()
            .compile();

        assertNotNull(binary);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("First"));
        assertTrue(decompiled.contains("Second"));
    }
}
