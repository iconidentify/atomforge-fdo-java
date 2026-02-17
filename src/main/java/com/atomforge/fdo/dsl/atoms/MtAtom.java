package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum MtAtom implements DslAtom {
    MANAGE_TOOLS,
    BOUNCE_TOOL,
    KILL_TOOL,
    DISPLAY_TOOL_INFO,
    LOAD_TOOL,
    BROWSE_TREE,
    FLIP_VALUE,
    TEST_CRASH,
    GID_TO_STRING,
    STRING_TO_GID,
    WAOLSOCK_DEBUG,
    BREAK,
    SET_SPY_MODE,
    SET_TIMEOUT_MODE,
    START_ATOMDEBUG,
    END_ATOMDEBUG,
    OPEN_DIAG,
    DELETE_ART_IN_TOP_FORM,
    HILIGHT_GROUPS_IN_TOP_FORM,
    PLAY_TEXT,
    PLAY_BINARY,
    DATABASE,
    START_EDIT_TEXT,
    SUPER_SNOOP;

    private final AtomDefinition def;

    MtAtom() {
        String atomName = "mt_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static MtAtom fromAtomNumber(int atomNumber) {
        for (MtAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static MtAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (MtAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
