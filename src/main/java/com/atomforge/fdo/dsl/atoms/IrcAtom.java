package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum IrcAtom implements DslAtom {
    MANAGE,
    ENTER,
    JOIN,
    CREATE,
    USER_INFO;

    private final AtomDefinition def;

    IrcAtom() {
        String atomName = "irc_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static IrcAtom fromAtomNumber(int atomNumber) {
        for (IrcAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static IrcAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (IrcAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
