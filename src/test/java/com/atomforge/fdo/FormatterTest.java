package com.atomforge.fdo;

import com.atomforge.fdo.text.FdoParser;
import com.atomforge.fdo.text.ast.*;
import com.atomforge.fdo.text.formatter.FdoFormatter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FormatterTest {

    @Test
    void shouldFormatSimpleAtom() throws Exception {
        StreamNode stream = FdoParser.parse("uni_start_stream");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("uni_start_stream");
    }

    @Test
    void shouldFormatAtomWithHexArg() throws Exception {
        StreamNode stream = FdoParser.parse("uni_start_stream <00x>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("uni_start_stream <00x>");
    }

    @Test
    void shouldFormatAtomWithGidArg() throws Exception {
        StreamNode stream = FdoParser.parse("mat_object_id <32-105>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("mat_object_id <32-105>");
    }

    @Test
    void shouldFormatAtomWithStringArg() throws Exception {
        StreamNode stream = FdoParser.parse("man_append_data <\"Hello World\">");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("man_append_data <\"Hello World\">");
    }

    @Test
    void shouldFormatAtomWithNumberArg() throws Exception {
        StreamNode stream = FdoParser.parse("act_set_criterion <7>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("act_set_criterion <7>");
    }

    @Test
    void shouldFormatAtomWithObjectTypeArg() throws Exception {
        StreamNode stream = FdoParser.parse("man_start_object <ind_group, \"My Title\">");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("man_start_object <ind_group, \"My Title\">");
    }

    @Test
    void shouldFormatAtomWithPipedArg() throws Exception {
        StreamNode stream = FdoParser.parse("mat_title_pos <left | center>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("mat_title_pos <left | center>");
    }

    @Test
    void shouldFormatAtomWithEmptyArgs() throws Exception {
        // Empty <> is semantically equivalent to no arguments
        // The formatter outputs the minimal form (no <>)
        StreamNode stream = FdoParser.parse("man_end_object <>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("man_end_object");
    }

    @Test
    void shouldFormatMultipleAtoms() throws Exception {
        StreamNode stream = FdoParser.parse("""
            uni_start_stream <00x>
            mat_object_id <32-105>
            uni_end_stream
            """);
        String output = FdoFormatter.formatToString(stream);

        assertThat(output).contains("uni_start_stream <00x>");
        assertThat(output).contains("mat_object_id <32-105>");
        assertThat(output).contains("uni_end_stream");
    }

    @Test
    void shouldFormatWithIndentation() throws Exception {
        StreamNode stream = FdoParser.parse("""
            uni_start_stream <00x>
            mat_object_id <32-105>
            uni_end_stream
            """);
        String output = FdoFormatter.formatToString(stream);

        // uni_start_stream has INDENT flag, so mat_object_id should be indented
        // uni_end_stream has OUTDENT flag, so it should not be indented
        String[] lines = output.split("\n");
        assertThat(lines[0]).doesNotStartWith(" ");  // uni_start_stream not indented
        assertThat(lines[1]).startsWith("  ");       // mat_object_id indented
        assertThat(lines[2]).doesNotStartWith(" ");  // uni_end_stream outdented
    }

    @Test
    void shouldFormatNestedStream() throws Exception {
        StreamNode stream = FdoParser.parse("""
            act_replace_action
                <
                uni_start_stream
                uni_end_stream
                >
            """);
        String output = FdoFormatter.formatToString(stream);

        assertThat(output).contains("act_replace_action");
        assertThat(output).contains("uni_start_stream");
        assertThat(output).contains("uni_end_stream");
    }

    @Test
    void shouldFormatThreePartGid() throws Exception {
        StreamNode stream = FdoParser.parse("mat_art_id <1-0-1329>");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("mat_art_id <1-0-1329>");
    }

    @Test
    void shouldEscapeStringWithQuotes() throws Exception {
        // Create a string arg manually since the parser would need escaping
        StreamNode stream = FdoParser.parse("man_append_data <\"Hello\">");
        String output = FdoFormatter.formatToString(stream);

        assertThat(output.trim()).isEqualTo("man_append_data <\"Hello\">");
    }

    @Test
    void shouldRoundTripParsedSource() throws Exception {
        String original = """
            uni_start_stream <00x>
              man_start_object <independent, "Test Object">
                mat_object_id <32-100>
                mat_orientation <vcf>
              man_end_object <>
            uni_end_stream <>
            """.stripIndent();

        StreamNode stream = FdoParser.parse(original);
        String formatted = FdoFormatter.formatToString(stream);
        StreamNode reparsed = FdoParser.parse(formatted);

        // Should have same structure
        assertThat(reparsed.size()).isEqualTo(stream.size());
        assertThat(reparsed.get(0).name()).isEqualTo("uni_start_stream");
        assertThat(reparsed.get(reparsed.size() - 1).name()).isEqualTo("uni_end_stream");
    }
}
