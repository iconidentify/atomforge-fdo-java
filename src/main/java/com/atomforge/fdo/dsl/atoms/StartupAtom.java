package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum StartupAtom implements DslAtom {
    ACTIVEX_MANAGE,
    ACTIVEX_ID,
    ACTIVEX_CLASSID,
    ACTIVEX_CODEBASE,
    ACTIVEX_DATA,
    ACTIVEX_SCRIPT_CODE,
    ACTIVEX_SCRIPT_EXPRESSION,
    ACTIVEX_SCRIPT_START,
    ACTIVEX_PARAM,
    ACTIVEX_SCRIPT_SHUTDOWN;

    private final AtomDefinition def;

    StartupAtom() {
        String atomName = "startup_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static StartupAtom fromAtomNumber(int atomNumber) {
        for (StartupAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static StartupAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (StartupAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
