package com.atomforge.fdo.dsl;

import com.atomforge.fdo.dsl.atoms.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DslTextEmitter quoting behavior.
 *
 * Verifies that atoms get correct quoting in FDO text output:
 * - Some atoms need their string args quoted (chat_add_user, sm_send_token_raw)
 * - Some atoms need their string args unquoted (act_set_criterion, uni_use_last_atom_string)
 */
class DslTextEmitterTest {

    @Test
    void emitsCriterionUnquoted() {
        // act_set_criterion has TOKENARG type which normally quotes,
        // but criterion keywords like "close" should be unquoted
        String source = FdoScript.stream()
            .act(ActAtom.SET_CRITERION, "close")
            .toSource();

        assertThat(source).contains("act_set_criterion <close>");
        assertThat(source).doesNotContain("\"close\"");
    }

    @Test
    void emitsDoActionUnquoted() {
        // act_do_action also takes criterion keywords
        String source = FdoScript.stream()
            .act(ActAtom.DO_ACTION, "select")
            .toSource();

        assertThat(source).contains("act_do_action <select>");
        assertThat(source).doesNotContain("\"select\"");
    }

    @Test
    void emitsTokenRawQuoted() {
        // sm_send_token_raw has TOKEN type which normally doesn't quote,
        // but token strings like "CL" should be quoted
        String source = FdoScript.stream()
            .sm(SmAtom.SEND_TOKEN_RAW, "CL")
            .toSource();

        assertThat(source).contains("sm_send_token_raw <\"CL\">");
    }

    @Test
    void emitsTokenArgQuoted() {
        // sm_send_token_arg should also quote
        String source = FdoScript.stream()
            .sm(SmAtom.SEND_TOKEN_ARG, "LP")
            .toSource();

        assertThat(source).contains("sm_send_token_arg <\"LP\">");
    }

    @Test
    void emitsChatUserQuoted() {
        // chat_add_user has RAW type which normally doesn't quote,
        // but usernames should be quoted strings
        String source = FdoScript.stream()
            .atom(ChatAtom.ADD_USER, "Grok")
            .toSource();

        assertThat(source).contains("chat_add_user <\"Grok\">");
    }

    @Test
    void emitsAtomRefUnquoted() {
        // uni_use_last_atom_string takes an atom name which should not be quoted
        String source = FdoScript.stream()
            .uni(UniAtom.USE_LAST_ATOM_STRING, "man_replace_data")
            .toSource();

        assertThat(source).contains("uni_use_last_atom_string <man_replace_data>");
        assertThat(source).doesNotContain("\"man_replace_data\"");
    }

    @Test
    void emitsAtomValueRefUnquoted() {
        // uni_use_last_atom_value also takes an atom name
        String source = FdoScript.stream()
            .uni(UniAtom.USE_LAST_ATOM_VALUE, "de_data")
            .toSource();

        assertThat(source).contains("uni_use_last_atom_value <de_data>");
        assertThat(source).doesNotContain("\"de_data\"");
    }

    @Test
    void emitsRegularStringQuoted() {
        // Regular STRING type atoms should still quote their strings
        String source = FdoScript.stream()
            .man(ManAtom.REPLACE_DATA, "Hello World")
            .toSource();

        assertThat(source).contains("man_replace_data <\"Hello World\">");
    }

    @Test
    void emitsNoArgsWithoutBrackets() {
        // Atoms with no arguments should not have empty brackets
        String source = FdoScript.stream()
            .uni(UniAtom.START_STREAM)
            .toSource();

        assertThat(source.trim()).isEqualTo("uni_start_stream");
        assertThat(source).doesNotContain("<>");
    }

    // ========== Phase 1: DslAtom typed arguments ==========

    @Test
    void emitsTypedDslAtomArg() {
        // uni_use_last_atom_string can accept a DslAtom directly instead of string
        // This provides compile-time type safety for atom references
        String source = FdoScript.stream()
            .uni(UniAtom.USE_LAST_ATOM_STRING, ManAtom.REPLACE_DATA)
            .toSource();

        assertThat(source).contains("uni_use_last_atom_string <man_replace_data>");
    }

    @Test
    void emitsTypedDslAtomValueArg() {
        // uni_use_last_atom_value also accepts DslAtom
        String source = FdoScript.stream()
            .uni(UniAtom.USE_LAST_ATOM_VALUE, MatAtom.VALUE)
            .toSource();

        assertThat(source).contains("uni_use_last_atom_value <mat_value>");
    }

    @Test
    void emitsTypedCriterionArg() {
        // act_set_criterion accepts Criterion enum directly
        String source = FdoScript.stream()
            .act(ActAtom.SET_CRITERION, com.atomforge.fdo.dsl.values.Criterion.SELECT)
            .toSource();

        assertThat(source).contains("act_set_criterion <select>");
    }

    // ========== Phase 2: CriterionArg wrapper tests ==========

    @Test
    void emitsCriterionArgFromKnownEnum() {
        // CriterionArg.of(Criterion) wraps the enum
        String source = FdoScript.stream()
            .setCriterion(com.atomforge.fdo.dsl.values.CriterionArg.of(
                com.atomforge.fdo.dsl.values.Criterion.CLOSE))
            .toSource();

        assertThat(source).contains("act_set_criterion <close>");
    }

    @Test
    void emitsCriterionArgFromRawString() {
        // CriterionArg.of(String) falls back to raw for unknown criteria
        String source = FdoScript.stream()
            .setCriterion(com.atomforge.fdo.dsl.values.CriterionArg.of("custom_criterion"))
            .toSource();

        assertThat(source).contains("act_set_criterion <custom_criterion>");
    }

    @Test
    void emitsCriterionArgFromNumericCode() {
        // CriterionArg.of(int) allows numeric codes for unknown criteria
        String source = FdoScript.stream()
            .setCriterion(com.atomforge.fdo.dsl.values.CriterionArg.of(130))
            .toSource();

        assertThat(source).contains("act_set_criterion <130>");
    }

    @Test
    void criterionArgAutoResolveKnownString() {
        // CriterionArg.of("select") should resolve to Known(SELECT)
        var arg = com.atomforge.fdo.dsl.values.CriterionArg.of("select");
        assertThat(arg).isInstanceOf(com.atomforge.fdo.dsl.values.CriterionArg.Known.class);
        assertThat(arg.fdoName()).isEqualTo("select");
    }

    @Test
    void criterionArgAutoResolveKnownCode() {
        // CriterionArg.of(2) should resolve to Known(CLOSE)
        var arg = com.atomforge.fdo.dsl.values.CriterionArg.of(2);
        assertThat(arg).isInstanceOf(com.atomforge.fdo.dsl.values.CriterionArg.Known.class);
        assertThat(arg.fdoName()).isEqualTo("close");
    }

    @Test
    void setCriterionConvenienceMethod() {
        // StreamBuilder.setCriterion(Criterion) convenience method
        String source = FdoScript.stream()
            .setCriterion(com.atomforge.fdo.dsl.values.Criterion.DOUBLE_CLICK)
            .toSource();

        assertThat(source).contains("act_set_criterion <double_click>");
    }
}
