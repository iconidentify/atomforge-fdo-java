package com.atomforge.fdo;

import com.atomforge.fdo.text.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LexerTest {

    @Test
    void shouldTokenizeSimpleAtom() throws Exception {
        FdoLexer lexer = new FdoLexer("uni_start_stream");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(2);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.ATOM_NAME);
        assertThat(tokens.get(0).value()).isEqualTo("uni_start_stream");
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void shouldTokenizeAtomWithAngleBrackets() throws Exception {
        FdoLexer lexer = new FdoLexer("mat_object_id <32-105>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.ATOM_NAME);
        assertThat(tokens.get(0).value()).isEqualTo("mat_object_id");
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.ANGLE_OPEN);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.GID);
        assertThat(tokens.get(2).value()).isEqualTo("32-105");
        assertThat(tokens.get(3).type()).isEqualTo(TokenType.ANGLE_CLOSE);
    }

    @Test
    void shouldTokenizeThreePartGid() throws Exception {
        FdoLexer lexer = new FdoLexer("<1-0-1329>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.GID);
        assertThat(tokens.get(1).value()).isEqualTo("1-0-1329");
    }

    @Test
    void shouldTokenizeStringWithQuotes() throws Exception {
        FdoLexer lexer = new FdoLexer("man_start_object <trigger, \"Hello World\">");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(7);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.ATOM_NAME);
        assertThat(tokens.get(3).type()).isEqualTo(TokenType.COMMA);
        assertThat(tokens.get(4).type()).isEqualTo(TokenType.STRING);
        assertThat(tokens.get(4).value()).isEqualTo("Hello World");
    }

    @Test
    void shouldTokenizeHexValue() throws Exception {
        FdoLexer lexer = new FdoLexer("uni_start_stream <00x>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.HEX_VALUE);
        assertThat(tokens.get(2).value()).isEqualTo("00x");
    }

    @Test
    void shouldTokenizeOrientationCode() throws Exception {
        FdoLexer lexer = new FdoLexer("mat_orientation <vcf>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(2).value()).isEqualTo("vcf");
    }

    @Test
    void shouldTokenizeNumber() throws Exception {
        FdoLexer lexer = new FdoLexer("act_set_criterion <7>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(2).value()).isEqualTo("7");
    }

    @Test
    void shouldTokenizeMultipleNumbers() throws Exception {
        FdoLexer lexer = new FdoLexer("mat_size <50, 4, 512>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(9);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(2).value()).isEqualTo("50");
        assertThat(tokens.get(4).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(4).value()).isEqualTo("4");
        assertThat(tokens.get(6).type()).isEqualTo(TokenType.NUMBER);
        assertThat(tokens.get(6).value()).isEqualTo("512");
    }

    @Test
    void shouldTokenizePipeForOr() throws Exception {
        FdoLexer lexer = new FdoLexer("mat_title_pos <left | center>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(7);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(2).value()).isEqualTo("left");
        assertThat(tokens.get(3).type()).isEqualTo(TokenType.PIPE);
        assertThat(tokens.get(4).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(4).value()).isEqualTo("center");
    }

    @Test
    void shouldTokenizeBooleanYes() throws Exception {
        FdoLexer lexer = new FdoLexer("mat_bool_disabled <yes>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(5);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.IDENTIFIER);
        assertThat(tokens.get(2).value()).isEqualTo("yes");
    }

    @Test
    void shouldTokenizeObjectType() throws Exception {
        FdoLexer lexer = new FdoLexer("man_start_object <ind_group, \"Title\">");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(7);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.ATOM_NAME);
        assertThat(tokens.get(2).value()).isEqualTo("ind_group");
        assertThat(tokens.get(4).type()).isEqualTo(TokenType.STRING);
        assertThat(tokens.get(4).value()).isEqualTo("Title");
    }

    @Test
    void shouldTrackLineNumbers() throws Exception {
        FdoLexer lexer = new FdoLexer("line1\nline2");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens.get(0).line()).isEqualTo(1);
        assertThat(tokens.get(2).line()).isEqualTo(2);
    }

    @Test
    void shouldHandleEmptyInput() throws Exception {
        FdoLexer lexer = new FdoLexer("");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.EOF);
    }

    @Test
    void shouldTokenizeEscapedString() throws Exception {
        FdoLexer lexer = new FdoLexer("\"Hello\\nWorld\"");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens.get(0).type()).isEqualTo(TokenType.STRING);
        assertThat(tokens.get(0).value()).isEqualTo("Hello\nWorld");
    }

    @Test
    void shouldTokenizeEmptyAngleBrackets() throws Exception {
        FdoLexer lexer = new FdoLexer("man_end_object <>");
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();

        assertThat(tokens).hasSize(4);
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.ATOM_NAME);
        assertThat(tokens.get(1).type()).isEqualTo(TokenType.ANGLE_OPEN);
        assertThat(tokens.get(2).type()).isEqualTo(TokenType.ANGLE_CLOSE);
    }

    @Test
    void shouldThrowOnUnterminatedString() {
        FdoLexer lexer = new FdoLexer("\"unterminated");
        assertThatThrownBy(() -> lexer.tokenizeSkipWhitespace())
            .isInstanceOf(FdoException.class)
            .hasMessageContaining("Unterminated string");
    }
}
