package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum XferAtom implements DslAtom {
    ABORT,
    SHOW_FILE_DESCRIPTION,
    SHOW_FILE_STATUS,
    START_DOWNLOAD,
    TOGGLE_SIGNOFF,
    FINISH_LATER,
    DECOMPRESS_FILE,
    SET_RLE_FLAG,
    CLEAR_RLE_FLAG,
    REFRESH_PREFS,
    BOOL_IN_PROGRESS,
    INVOKE_DL_MANAGER,
    DELETE_SPOOL_FILE,
    START_OBJECT,
    ATR_REQUEST_ID,
    ATR_FILE_SIZE,
    ATR_TITLE,
    ATR_FILE_NAME,
    END_OBJECT,
    INVOKE_ARCHIVE,
    ATR_LIBRARY,
    BOOL_MAIL,
    ATR_CREATE_DATE,
    BATCH_DOWNLOAD,
    LOCATE_FILE,
    SET_NO_DIALOGS;

    private final AtomDefinition def;

    XferAtom() {
        String atomName = "xfer_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static XferAtom fromAtomNumber(int atomNumber) {
        for (XferAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static XferAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (XferAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
