package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum DocAtom implements DslAtom {
    MANAGE,
    EDIT_ACTION,
    ADD_URL,
    SET_COLOR_ITEM,
    BOOL_READ_ONLY,
    OPEN_FILE,
    REMOVE_URL,
    GET_CURRENT_URL,
    GET_CURRENT_LINK,
    SET_FONT_NAME,
    SET_FONT_SIZE,
    SET_FONT_STYLE,
    CLEAR_FONT_STYLE,
    SET_TEXT_ALIGNMENT,
    GET_FONT_NAME,
    GET_FONT_SIZE,
    GET_FONT_STYLE,
    GET_TEXT_ALIGNMENT;

    private final AtomDefinition def;

    DocAtom() {
        String atomName = "doc_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static DocAtom fromAtomNumber(int atomNumber) {
        for (DocAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static DocAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (DocAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
