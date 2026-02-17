package com.atomforge.fdo;

import com.atomforge.fdo.text.FdoParser;
import com.atomforge.fdo.text.ast.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ParserTest {

    @Test
    void shouldParseSimpleAtom() throws Exception {
        StreamNode stream = FdoParser.parse("uni_start_stream");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.name()).isEqualTo("uni_start_stream");
        assertThat(atom.hasDefinition()).isTrue();
        assertThat(atom.protocol()).isEqualTo(0); // UNI
        assertThat(atom.atomNumber()).isEqualTo(1);
    }

    @Test
    void shouldParseAtomWithGidArgument() throws Exception {
        StreamNode stream = FdoParser.parse("mat_object_id <32-105>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.name()).isEqualTo("mat_object_id");
        assertThat(atom.arguments()).hasSize(1);

        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.GidArg.class);
        ArgumentNode.GidArg gid = (ArgumentNode.GidArg) arg;
        assertThat(gid.value()).isEqualTo("32-105");
        assertThat(gid.type()).isEqualTo(32);
        assertThat(gid.id()).isEqualTo(105);
    }

    @Test
    void shouldParseAtomWithHexArgument() throws Exception {
        StreamNode stream = FdoParser.parse("uni_start_stream <00x>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.HexArg.class);
        assertThat(((ArgumentNode.HexArg) arg).value()).isEqualTo("00x");
    }

    @Test
    void shouldParseAtomWithStringArgument() throws Exception {
        StreamNode stream = FdoParser.parse("man_append_data <\"Hello World\">");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.StringArg.class);
        assertThat(((ArgumentNode.StringArg) arg).value()).isEqualTo("Hello World");
    }

    @Test
    void shouldParseAtomWithObjectTypeArgument() throws Exception {
        StreamNode stream = FdoParser.parse("man_start_object <ind_group, \"My Title\">");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.arguments()).hasSize(1);

        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.ObjectTypeArg.class);
        ArgumentNode.ObjectTypeArg objType = (ArgumentNode.ObjectTypeArg) arg;
        assertThat(objType.objectType()).isEqualTo("ind_group");
        assertThat(objType.title()).isEqualTo("My Title");
    }

    @Test
    void shouldParseAtomWithNumberArgument() throws Exception {
        StreamNode stream = FdoParser.parse("act_set_criterion <7>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.NumberArg.class);
        assertThat(((ArgumentNode.NumberArg) arg).value()).isEqualTo(7);
    }

    @Test
    void shouldParseAtomWithMultipleNumberArguments() throws Exception {
        StreamNode stream = FdoParser.parse("mat_size <50, 4, 512>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.arguments()).hasSize(1);

        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.ListArg.class);
        ArgumentNode.ListArg list = (ArgumentNode.ListArg) arg;
        assertThat(list.elements()).hasSize(3);
    }

    @Test
    void shouldParseAtomWithPipedArgument() throws Exception {
        StreamNode stream = FdoParser.parse("mat_title_pos <left | center>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.PipedArg.class);
        ArgumentNode.PipedArg piped = (ArgumentNode.PipedArg) arg;
        assertThat(piped.parts()).hasSize(2);
        assertThat(piped.parts().get(0)).isInstanceOf(ArgumentNode.IdentifierArg.class);
        assertThat(((ArgumentNode.IdentifierArg) piped.parts().get(0)).value()).isEqualTo("left");
        assertThat(((ArgumentNode.IdentifierArg) piped.parts().get(1)).value()).isEqualTo("center");
    }

    @Test
    void shouldParseAtomWithEmptyArguments() throws Exception {
        StreamNode stream = FdoParser.parse("man_end_object <>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.arguments()).isEmpty();
    }

    @Test
    void shouldParseAtomWithNoArguments() throws Exception {
        StreamNode stream = FdoParser.parse("man_update_display");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.arguments()).isEmpty();
    }

    @Test
    void shouldParseMultipleAtoms() throws Exception {
        StreamNode stream = FdoParser.parse("""
            uni_start_stream <00x>
            mat_object_id <32-105>
            uni_end_stream
            """);

        assertThat(stream.size()).isEqualTo(3);
        assertThat(stream.get(0).name()).isEqualTo("uni_start_stream");
        assertThat(stream.get(1).name()).isEqualTo("mat_object_id");
        assertThat(stream.get(2).name()).isEqualTo("uni_end_stream");
    }

    @Test
    void shouldParseNestedStream() throws Exception {
        StreamNode stream = FdoParser.parse("""
            act_replace_action
                <
                uni_start_stream
                uni_end_stream
                >
            """);

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        assertThat(atom.name()).isEqualTo("act_replace_action");
        assertThat(atom.arguments()).hasSize(1);

        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.NestedStreamArg.class);
        ArgumentNode.NestedStreamArg nested = (ArgumentNode.NestedStreamArg) arg;
        assertThat(nested.stream().size()).isEqualTo(2);
        assertThat(nested.stream().get(0).name()).isEqualTo("uni_start_stream");
        assertThat(nested.stream().get(1).name()).isEqualTo("uni_end_stream");
    }

    @Test
    void shouldParseOrientationCode() throws Exception {
        StreamNode stream = FdoParser.parse("mat_orientation <vcf>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.IdentifierArg.class);
        assertThat(((ArgumentNode.IdentifierArg) arg).value()).isEqualTo("vcf");
    }

    @Test
    void shouldParseBooleanYes() throws Exception {
        StreamNode stream = FdoParser.parse("mat_bool_disabled <yes>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.IdentifierArg.class);
        assertThat(((ArgumentNode.IdentifierArg) arg).value()).isEqualTo("yes");
    }

    @Test
    void shouldParseThreePartGid() throws Exception {
        StreamNode stream = FdoParser.parse("mat_art_id <1-0-1329>");

        assertThat(stream.size()).isEqualTo(1);
        AtomNode atom = stream.get(0);
        ArgumentNode arg = atom.firstArgument().orElseThrow();
        assertThat(arg).isInstanceOf(ArgumentNode.GidArg.class);
        ArgumentNode.GidArg gid = (ArgumentNode.GidArg) arg;
        assertThat(gid.type()).isEqualTo(1);
        assertThat(gid.subtype()).isEqualTo(0);
        assertThat(gid.id()).isEqualTo(1329);
    }
}
