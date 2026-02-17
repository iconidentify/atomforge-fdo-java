package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum WwwAtom implements DslAtom {
    LOAD_URL,
    GO_BACK,
    GO_FORWARD,
    RELOAD,
    STOP,
    GET_CURRENT_URL,
    GET_CURRENT_LINK,
    GET_PARTS_COMPLETE,
    CAN_GO_BACK,
    CAN_GO_FORWARD,
    GET_CURRENT_TITLE,
    BROWSE,
    MANAGE,
    REFRESH_PREFS,
    PURGE_CACHE,
    GET_STATUS_MSG,
    BOOL_SUPPRESS_ERRORS,
    INVOKE_OPTIONS,
    GET_CURRENT_FULL_URL,
    DISPLAY_3DBEVEL,
    ACTION_COMMAND,
    SET_HTTPS_PROXY,
    SET_PROXY_HTTP;

    private final AtomDefinition def;

    WwwAtom() {
        String atomName = "www_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static WwwAtom fromAtomNumber(int atomNumber) {
        for (WwwAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static WwwAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (WwwAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
