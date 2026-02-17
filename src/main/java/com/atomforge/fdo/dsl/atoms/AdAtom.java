package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum AdAtom implements DslAtom {
    ADD,
    STORE,
    MODIFY,
    SET_CALLER,
    TO,
    CC;

    private final AtomDefinition def;

    AdAtom() {
        String atomName = "ad_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static AdAtom fromAtomNumber(int atomNumber) {
        for (AdAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static AdAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (AdAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
