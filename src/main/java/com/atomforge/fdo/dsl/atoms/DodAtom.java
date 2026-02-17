package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum DodAtom implements DslAtom {
    START,
    TYPE,
    GID,
    FORM_ID,
    END,
    DATA,
    GAIN_FOCUS,
    CLOSE_FORM,
    NOT_AVAILABLE,
    HINTS,
    LOSE_FOCUS,
    NO_HINTS,
    END_FORM,
    END_DATA,
    HINTS_NO_ACTION,
    TAG,
    HINTS_MISMATCH;

    private final AtomDefinition def;

    DodAtom() {
        String atomName = "dod_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static DodAtom fromAtomNumber(int atomNumber) {
        for (DodAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static DodAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (DodAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
