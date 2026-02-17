package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum BlankAtom implements DslAtom {
    TEST,
    GET_FORM,
    GET_VER;

    private final AtomDefinition def;

    BlankAtom() {
        String atomName = "blank_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static BlankAtom fromAtomNumber(int atomNumber) {
        for (BlankAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static BlankAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (BlankAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
