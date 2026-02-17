package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum SmAtom implements DslAtom {
    DO_LIST_ACTION,
    END_OBJECT_WITH_ACTION,
    END_OBJECT_WITH_VAR,
    SEND_K1,
    SEND_ER,
    SEND_MR,
    SEND_MF,
    SEND_BM,
    SEND_BN,
    SEND_F1,
    CHECK_DOMAIN,
    SET_DOMAIN,
    SEND_TOKEN_ARG,
    P_SEND_TOKEN_ARG,
    MP_SEND_TOKEN_ARG,
    SEND_TOKEN_RAW,
    M_SEND_TOKEN_RAW,
    SEND_WINDOW_VAR,
    M_SEND_WINDOW_VAR,
    P_SEND_WINDOW_VAR,
    MP_SEND_WINDOW_VAR,
    SEND_LIST_VAR,
    M_SEND_LIST_VAR,
    M_SEND_TOKEN_ARG,
    IDB_GET_DATA,
    SET_OBJECT_DOMAIN,
    SEND_FREE_K1,
    SEND_PAID_K1,
    SEND_FREE_F1,
    SEND_PAID_F1,
    SET_PLUS_GROUP,
    CHECK_PLUS_GROUP,
    ENTER_PAID,
    ENTER_FREE,
    SEND_SELECTION_CODE;

    private final AtomDefinition def;

    SmAtom() {
        String atomName = "sm_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static SmAtom fromAtomNumber(int atomNumber) {
        for (SmAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static SmAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (SmAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
