package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum BufAtom implements DslAtom {
    START_BUFFER,
    ADD_ATOM_POINTER,
    ADD_ATOM_DATA,
    ADD_POINTER,
    ADD_DATA,
    ADD_TOKEN,
    SET_TOKEN,
    CLEAR_BUFFER,
    CLOSE_BUFFER,
    SAVE_BUFFER_TO_DB,
    LOAD_BUFFER_FROM_DB,
    SEND_BUFFER_TO_HOST,
    SET_FLAGS,
    ADD_ATOM_POINTER_FILTER,
    USE_BUFFER,
    ADD_ATOM_DATA_TRIM,
    ADD_DATA_TRIM,
    SET_DATA_ATOM,
    DESTROY_BUFFER,
    ADD_STRING_RAW,
    ADD_DATA_RAW,
    SET_PROTOCOL,
    GET_FLAGS,
    GET_DATA_ATOM,
    GET_PROTOCOL,
    START_SCRBUF,
    END_SCRBUF,
    ADD_STRING_TO_SCRBUF,
    ADD_VALUE_TO_SCRBUF,
    GET_SCRBUF,
    SET_CALLBACK,
    DEBUG_DUMP_BUFFERS,
    RESTART_BUFFER,
    ADD_ATOM_TYPED_DATA,
    ADD_ATOM_POINTER_FLIP,
    ADD_ATOM_DATA_FLIP,
    ADD_DATA_FLIP,
    SET_SECURITY_TOKENS,
    METER_ABORT;

    private final AtomDefinition def;

    BufAtom() {
        String atomName = "buf_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static BufAtom fromAtomNumber(int atomNumber) {
        for (BufAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static BufAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (BufAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
