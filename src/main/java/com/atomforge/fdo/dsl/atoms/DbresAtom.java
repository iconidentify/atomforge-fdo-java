package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum DbresAtom implements DslAtom {
    MERC_MANAGE,
    MERC_SELECT_BANK,
    MERC_SETTINGS,
    MERC_HELP,
    MERC_ABOUT,
    MERC_CLOSE;

    private final AtomDefinition def;

    DbresAtom() {
        String atomName = "dbres_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static DbresAtom fromAtomNumber(int atomNumber) {
        for (DbresAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static DbresAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (DbresAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
