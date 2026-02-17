package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum VidAtom implements DslAtom {
    CAPTURE,
    SEND,
    MANAGE,
    PREVIEW,
    REPEAT_SEND,
    SET_TOKEN,
    SET_UDP,
    SET_TCP,
    RECEIVE,
    ACTIVATE_CAPTURE,
    CAN_PREVIEW,
    SET_FORMAT,
    RECEIVE_ENABLE,
    IS_AVAILABLE,
    PREV_COUNT,
    NEXT_COUNT,
    SHOW_PREV,
    SHOW_NEXT,
    SHOW_OLDEST,
    SHOW_NEWEST,
    FLAG_SETUP,
    SETUP,
    OPEN,
    SAVE_AS,
    SETUP_POPUP,
    GET_DATA,
    ENABLE_EXTRACT,
    QUERY_SETUP,
    STORE_SETUP;

    private final AtomDefinition def;

    VidAtom() {
        String atomName = "vid_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static VidAtom fromAtomNumber(int atomNumber) {
        for (VidAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static VidAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (VidAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
