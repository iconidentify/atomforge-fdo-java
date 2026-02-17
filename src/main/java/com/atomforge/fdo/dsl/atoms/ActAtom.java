package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ActAtom implements DslAtom {
    SET_CRITERION,
    DO_ACTION,
    SET_INHERITANCE,
    REPLACE_ACTION,
    REPLACE_SELECT_ACTION,
    SOUND_BEEP,
    MODIFY_ACTION,
    SET_TEST_INDEX,
    CLR_TEST_INDEX,
    SET_INDEX,
    APPEND_ACTION,
    PREPEND_ACTION,
    CHANGE_ACTION,
    APPEND_SELECT_ACTION,
    PREPEND_SELECT_ACTION,
    CHANGE_SELECT_ACTION,
    COPY_ACTION_TO_REG,
    REPLACE_ACTION_FROM_REG,
    APPEND_ACTION_FROM_REG,
    PREPEND_ACTION_FROM_REG,
    CHANGE_ACTION_FROM_REG,
    SET_ACTION_IN_REG,
    INTERPRET_PACKET,
    SET_DB_LENGTH,
    GET_DB_RECORD,
    SET_DB_ID,
    SET_DB_RECORD,
    SET_GUEST_FLAG,
    SET_NEWUSER_FLAG,
    SET_DB_OFFSET,
    GET_DB_VALUE,
    FORMAT_QUOTE,
    REPLACE_POPUP_MENU_ACTION;

    private final AtomDefinition def;

    ActAtom() {
        String atomName = "act_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ActAtom fromAtomNumber(int atomNumber) {
        for (ActAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ActAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ActAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
