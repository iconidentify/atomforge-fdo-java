package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum DeAtom implements DslAtom {
    SET_EXTRACTION_TYPE,
    DATA,
    START_EXTRACTION,
    SET_DATA_TYPE,
    SET_VARIABLE_ID,
    SET_TEXT_COLUMN,
    GET_DATA,
    END_EXTRACTION,
    GET_DATA_VALUE,
    GET_DATA_POINTER,
    EZ_SEND_FORM,
    CUSTOM_DATA,
    EZ_SEND_LIST_TEXT,
    EZ_SEND_LIST_INDEX,
    EZ_SEND_FIELD,
    VALIDATE,
    TYPED_DATA;

    private final AtomDefinition def;

    DeAtom() {
        String atomName = "de_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static DeAtom fromAtomNumber(int atomNumber) {
        for (DeAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static DeAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (DeAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
