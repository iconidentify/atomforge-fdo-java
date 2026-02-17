package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum PictalkAtom implements DslAtom {
    MANAGE,
    OPEN_FILE,
    CONTROL,
    GET_ATTR_STYLE,
    ATTR_STYLE,
    TIMELINE_ACTION,
    DELETE_TIMELINE,
    ATTR_INTL,
    ATTR_CTRL_ID,
    ATTR_ART_ID,
    UPDATE_ART,
    RESET_STYLES,
    AD_MANAGE;

    private final AtomDefinition def;

    PictalkAtom() {
        String atomName = "pictalk_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static PictalkAtom fromAtomNumber(int atomNumber) {
        for (PictalkAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static PictalkAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (PictalkAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
