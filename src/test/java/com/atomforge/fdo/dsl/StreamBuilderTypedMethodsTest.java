package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.atoms.*;
import com.atomforge.fdo.dsl.values.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests for the per-atom typed methods in StreamBuilder.
 *
 * These methods provide compile-time type safety by accepting specific
 * enum types rather than generic Object arguments.
 */
class StreamBuilderTypedMethodsTest {

    // ========== ACT Protocol Tests ==========

    @Test
    @DisplayName("actSetCriterion(Criterion) produces correct output")
    void testActSetCriterionTyped() throws FdoException {
        String source = FdoScript.stream()
            .actSetCriterion(Criterion.CLOSE)
            .toSource();

        assertThat(source).contains("act_set_criterion <close>");
    }

    @Test
    @DisplayName("actSetCriterion(CriterionArg) handles raw values")
    void testActSetCriterionArg() throws FdoException {
        String source = FdoScript.stream()
            .actSetCriterion(CriterionArg.of(130))
            .toSource();

        assertThat(source).contains("act_set_criterion <130>");
    }

    @Test
    @DisplayName("actDoAction(Criterion) produces correct output")
    void testActDoActionTyped() throws FdoException {
        String source = FdoScript.stream()
            .actDoAction(Criterion.SELECT)
            .toSource();

        assertThat(source).contains("act_do_action <select>");
    }

    // ========== MAN Protocol Tests ==========

    @Test
    @DisplayName("manStartObject(ObjectType) produces correct output")
    void testManStartObjectTyped() throws FdoException {
        String source = FdoScript.stream()
            .manStartObject(ObjectType.IND_GROUP)
            .toSource();

        // When no title provided, empty string is included
        assertThat(source).contains("man_start_object <ind_group");
    }

    @Test
    @DisplayName("manStartObject(ObjectType, String) includes title")
    void testManStartObjectWithTitle() throws FdoException {
        String source = FdoScript.stream()
            .manStartObject(ObjectType.TRIGGER, "My Button")
            .toSource();

        assertThat(source).contains("man_start_object <trigger, \"My Button\">");
    }

    @Test
    @DisplayName("manStartSibling(ObjectType) produces correct output")
    void testManStartSiblingTyped() throws FdoException {
        String source = FdoScript.stream()
            .manStartSibling(ObjectType.ORNAMENT)
            .toSource();

        // When no title provided, empty string is included
        assertThat(source).contains("man_start_sibling <ornament");
    }

    // ========== MAT Protocol Tests ==========

    @Test
    @DisplayName("matOrientation(Orientation) produces correct output")
    void testMatOrientationTyped() throws FdoException {
        String source = FdoScript.stream()
            .matOrientation(Orientation.VCF)
            .toSource();

        assertThat(source).contains("mat_orientation <vcf>");
    }

    @Test
    @DisplayName("matPosition(Position) produces correct output")
    void testMatPositionTyped() throws FdoException {
        String source = FdoScript.stream()
            .matPosition(Position.CENTER_CENTER)
            .toSource();

        assertThat(source).contains("mat_position <center_center>");
    }

    @Test
    @DisplayName("matFontId(FontId) produces correct output")
    void testMatFontIdTyped() throws FdoException {
        String source = FdoScript.stream()
            .matFontId(FontId.ARIAL)
            .toSource();

        assertThat(source).contains("mat_font_id <arial>");
    }

    @Test
    @DisplayName("matFontStyle(FontStyle) produces correct output")
    void testMatFontStyleTyped() throws FdoException {
        String source = FdoScript.stream()
            .matFontStyle(FontStyle.BOLD)
            .toSource();

        assertThat(source).contains("mat_font_style <bold>");
    }

    @Test
    @DisplayName("matTitlePos(TitlePosition) produces correct output")
    void testMatTitlePosTyped() throws FdoException {
        String source = FdoScript.stream()
            .matTitlePos(TitlePosition.LEFT_CENTER)
            .toSource();

        // Piped format uses no spaces: "left|center"
        assertThat(source).contains("mat_title_pos <left|center>");
    }

    @Test
    @DisplayName("matLogObject(LogType) produces correct output")
    void testMatLogObjectTyped() throws FdoException {
        String source = FdoScript.stream()
            .matLogObject(LogType.CHAT_LOG)
            .toSource();

        assertThat(source).contains("mat_log_object <chat_log>");
    }

    @Test
    @DisplayName("matSortOrder(SortOrder) produces correct output")
    void testMatSortOrderTyped() throws FdoException {
        String source = FdoScript.stream()
            .matSortOrder(SortOrder.ALPHABETICAL)
            .toSource();

        assertThat(source).contains("mat_sort_order <alphabetical>");
    }

    // ========== UNI Protocol Tests ==========

    @Test
    @DisplayName("uniUseLastAtomString(DslAtom) produces correct output")
    void testUniUseLastAtomStringTyped() throws FdoException {
        String source = FdoScript.stream()
            .uniUseLastAtomString(ManAtom.REPLACE_DATA)
            .toSource();

        assertThat(source).contains("uni_use_last_atom_string <man_replace_data>");
    }

    @Test
    @DisplayName("uniUseLastAtomValue(DslAtom) produces correct output")
    void testUniUseLastAtomValueTyped() throws FdoException {
        String source = FdoScript.stream()
            .uniUseLastAtomValue(MatAtom.VALUE)
            .toSource();

        assertThat(source).contains("uni_use_last_atom_value <mat_value>");
    }

    // ========== Byte-Identical Output Tests ==========

    @Test
    @DisplayName("Typed methods produce byte-identical output to generic methods")
    void testTypedMethodsProduceByteIdenticalOutput() throws FdoException {
        // Build using generic methods
        byte[] genericOutput = FdoScript.stream()
            .act(ActAtom.SET_CRITERION, Criterion.CLOSE)
            .man(ManAtom.START_OBJECT, ObjectType.IND_GROUP, "Test")
            .mat(MatAtom.ORIENTATION, Orientation.VCF)
            .mat(MatAtom.POSITION, Position.CENTER_CENTER)
            .uni(UniAtom.USE_LAST_ATOM_STRING, ManAtom.REPLACE_DATA)
            .compile();

        // Build using new typed methods
        byte[] typedOutput = FdoScript.stream()
            .actSetCriterion(Criterion.CLOSE)
            .manStartObject(ObjectType.IND_GROUP, "Test")
            .matOrientation(Orientation.VCF)
            .matPosition(Position.CENTER_CENTER)
            .uniUseLastAtomString(ManAtom.REPLACE_DATA)
            .compile();

        assertArrayEquals(genericOutput, typedOutput,
            "Typed methods must produce byte-identical output to deprecated generic methods");
    }

    @Test
    @DisplayName("Typed methods match direct text compilation")
    void testTypedMethodsMatchTextCompilation() throws FdoException {
        String fdoSource = """
            act_set_criterion <close>
            man_start_object <trigger, "Button">
            mat_orientation <hlt>
            man_end_object
            """;

        byte[] textOutput = FdoCompiler.create().compile(fdoSource);

        byte[] typedOutput = FdoScript.stream()
            .actSetCriterion(Criterion.CLOSE)
            .manStartObject(ObjectType.TRIGGER, "Button")
            .matOrientation(Orientation.HLT)
            .endObject()
            .compile();

        assertArrayEquals(textOutput, typedOutput,
            "Typed methods must produce identical output to text compilation");
    }

    // ========== Compilation Safety Tests ==========

    // Note: These tests document that INCORRECT usage won't compile.
    // We can't actually test compile errors in JUnit, but we document the safety:
    //
    // The following would NOT compile (which is the goal):
    //   .actSetCriterion(ManAtom.CLOSE)      // ManAtom is not Criterion
    //   .matOrientation(Position.CENTER)      // Position is not Orientation
    //   .uniUseLastAtomString("some_string")  // String is not DslAtom
    //
    // Users must use the correct types, providing compile-time safety.
}
