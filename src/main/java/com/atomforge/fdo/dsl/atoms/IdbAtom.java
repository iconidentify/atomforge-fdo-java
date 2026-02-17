package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum IdbAtom implements DslAtom {
    ATR_DOD,
    START_OBJ,
    END_OBJ,
    DELETE_OBJ,
    CLOSE_OBJ,
    EXISTS,
    START_EXTRACTION,
    END_EXTRACTION,
    GET_DATA,
    GET_VALUE,
    DOD_FAILED,
    APPEND_DATA,
    DATA,
    CHANGE_CONTEXT,
    END_CONTEXT,
    RESET,
    GET_STRING,
    CANCEL,
    SET_CONTEXT,
    ATR_GLOBALID,
    ATR_LENGTH,
    ATR_STAMP,
    ATR_OFFSET,
    ATR_TYPE,
    ATR_COMPRESSED,
    ATR_ENCRYPTED,
    ATR_EXPIRATION,
    ATR_COMPRESS,
    USE_DEFAULT_ICON,
    GET_LENGTH,
    DOD_PROGRESS_GAUGE,
    GET_DATA_NULL_TERMINATED,
    CHECK_AND_SET_FTV,
    CLEAR_FTV;

    private final AtomDefinition def;

    IdbAtom() {
        String atomName = "idb_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static IdbAtom fromAtomNumber(int atomNumber) {
        for (IdbAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static IdbAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (IdbAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
