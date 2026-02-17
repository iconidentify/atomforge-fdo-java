package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum MmiAtom implements DslAtom {
    OPEN_FILE,
    ATR_TOOLBAR,
    ATR_RID_PLAY,
    ATR_RID_PAUSE,
    ATR_RID_STOP,
    ATR_RID_REWIND,
    PLAY,
    PAUSE,
    STOP,
    REWIND,
    START_DATA,
    APPEND_DATA,
    END_DATA,
    ATR_TYPE,
    DB_ID,
    OPEN_FILE_RELATIVE,
    ATR_RID_COUNTER_DIGIT,
    ATR_LOOP,
    MANAGE,
    REFRESH_PREFS,
    SET_ATTR_PRIORITY,
    GET_ATTR_PRIORITY,
    XFER_DATA,
    CANCEL_XFER,
    PLAYCD;

    private final AtomDefinition def;

    MmiAtom() {
        String atomName = "mmi_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static MmiAtom fromAtomNumber(int atomNumber) {
        for (MmiAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static MmiAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (MmiAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
