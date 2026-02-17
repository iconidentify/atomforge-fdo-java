package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum CclAtom implements DslAtom {
    DIAL,
    HANG_UP,
    INSTALL_MODEM_DRIVER,
    UPDATE_LOCALITY,
    GET_LOCALITY,
    NEW_LOCATION,
    TRANSLATE_COM_PORT,
    TRANSLATE_BAUD_RATE,
    TRANSLATE_NETWORK,
    TRANSLATE_LOCALITY,
    CHECK_HANG_UP,
    IS_MODEM,
    ADD_NET_CHOICES,
    LIST_SET_NET,
    LIST_GET_NET,
    RELOAD_NETWORKS,
    ENUM_COM_DEVICES,
    CANCEL_ENUM_DEVICES,
    SELECT_COM_DEVICE;

    private final AtomDefinition def;

    CclAtom() {
        String atomName = "ccl_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static CclAtom fromAtomNumber(int atomNumber) {
        for (CclAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static CclAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (CclAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
