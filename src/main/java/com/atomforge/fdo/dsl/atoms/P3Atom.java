package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum P3Atom implements DslAtom {
    START,
    STOP,
    WRITE,
    INTERLEAVED_MODE,
    DEBUG_DISABLE_OUTBOUND,
    DEBUG_GET_OUTBOUND_STATE;

    private final AtomDefinition def;

    P3Atom() {
        String atomName = "p3_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static P3Atom fromAtomNumber(int atomNumber) {
        for (P3Atom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static P3Atom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (P3Atom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
