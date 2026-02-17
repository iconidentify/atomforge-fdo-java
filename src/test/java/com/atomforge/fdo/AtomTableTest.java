package com.atomforge.fdo;

import com.atomforge.fdo.atom.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class AtomTableTest {

    private AtomTable table;

    @BeforeEach
    void setUp() {
        table = AtomTable.loadDefault();
    }

    @Test
    void shouldLoadAllAtoms() {
        assertThat(table.size()).isGreaterThan(400);
    }

    @Test
    void shouldFindUniStartStream() {
        Optional<AtomDefinition> atom = table.findByName("uni_start_stream");
        assertThat(atom).isPresent();
        assertThat(atom.get().protocol()).isEqualTo(Protocol.UNI);
        assertThat(atom.get().atomNumber()).isEqualTo(1);
        assertThat(atom.get().type()).isEqualTo(AtomType.RAW);
        assertThat(atom.get().isIndent()).isTrue();
    }

    @Test
    void shouldFindUniEndStream() {
        Optional<AtomDefinition> atom = table.findByName("uni_end_stream");
        assertThat(atom).isPresent();
        assertThat(atom.get().protocol()).isEqualTo(Protocol.UNI);
        assertThat(atom.get().atomNumber()).isEqualTo(2);
        assertThat(atom.get().isEos()).isTrue();
        assertThat(atom.get().isOutdent()).isTrue();
    }

    @Test
    void shouldFindManStartObject() {
        Optional<AtomDefinition> atom = table.findByName("man_start_object");
        assertThat(atom).isPresent();
        assertThat(atom.get().protocol()).isEqualTo(Protocol.MAN);
        assertThat(atom.get().atomNumber()).isEqualTo(0);
        assertThat(atom.get().type()).isEqualTo(AtomType.OBJSTART);
    }

    @Test
    void shouldFindMatObjectId() {
        Optional<AtomDefinition> atom = table.findByName("mat_object_id");
        assertThat(atom).isPresent();
        assertThat(atom.get().protocol()).isEqualTo(Protocol.MAT);
        assertThat(atom.get().atomNumber()).isEqualTo(12);
        assertThat(atom.get().type()).isEqualTo(AtomType.GID);
    }

    @Test
    void shouldFindActSetCriterion() {
        Optional<AtomDefinition> atom = table.findByName("act_set_criterion");
        assertThat(atom).isPresent();
        assertThat(atom.get().protocol()).isEqualTo(Protocol.ACT);
        assertThat(atom.get().atomNumber()).isEqualTo(0);
        // Type from ADA.BIN is TOKENARG (0x0A)
        assertThat(atom.get().type()).isEqualTo(AtomType.TOKENARG);
    }

    @Test
    void shouldFindActReplaceAction() {
        Optional<AtomDefinition> atom = table.findByName("act_replace_action");
        assertThat(atom).isPresent();
        // Type from ADA.BIN is STREAM (0x06) with INDENT flag
        assertThat(atom.get().type()).isEqualTo(AtomType.STREAM);
        assertThat(atom.get().isIndent()).isTrue();
    }

    @Test
    void shouldFindByProtocolAndAtom() {
        // UNI_START_STREAM is protocol 0, atom 1
        Optional<AtomDefinition> atom = table.findByProtocolAtom(0, 1);
        assertThat(atom).isPresent();
        assertThat(atom.get().name()).isEqualTo("uni_start_stream");
    }

    @Test
    void shouldBeCaseInsensitive() {
        assertThat(table.findByName("UNI_START_STREAM")).isPresent();
        assertThat(table.findByName("Uni_Start_Stream")).isPresent();
        assertThat(table.findByName("uni_start_stream")).isPresent();
    }

    @Test
    void shouldReturnEmptyForUnknownAtom() {
        assertThat(table.findByName("nonexistent_atom")).isEmpty();
        assertThat(table.findByProtocolAtom(99, 99)).isEmpty();
    }

    @Test
    void shouldStreamAllAtoms() {
        long count = table.allAtoms().count();
        assertThat(count).isEqualTo(table.size());
    }

    @Test
    void shouldFilterByProtocol() {
        long uniCount = table.atomsForProtocol(Protocol.UNI).count();
        assertThat(uniCount).isGreaterThan(20);

        long matCount = table.atomsForProtocol(Protocol.MAT).count();
        assertThat(matCount).isGreaterThan(50);
    }
}
