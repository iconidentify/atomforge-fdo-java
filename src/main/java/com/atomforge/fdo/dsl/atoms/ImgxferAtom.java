package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ImgxferAtom implements DslAtom {
    SET_CONTEXT,
    END_CONTEXT,
    SET_RID,
    SET_GID,
    ATTR_WAIT_ACTIVE,
    IS_SPOOLFILE,
    GET_SPOOLFILE_NAME,
    PRESET_KEEP_SPOOL;

    private final AtomDefinition def;

    ImgxferAtom() {
        String atomName = "imgxfer_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ImgxferAtom fromAtomNumber(int atomNumber) {
        for (ImgxferAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ImgxferAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ImgxferAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
