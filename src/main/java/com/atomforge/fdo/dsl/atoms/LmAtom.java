package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum LmAtom implements DslAtom {
    OPEN_WINDOW,
    CLOSE_WINDOW,
    USER_DELETE_ITEM,
    START_LIST,
    END_LIST,
    START_LIST_ENTRY,
    END_LIST_ENTRY,
    ATTR_LIST_TYPE,
    ATTR_LIST_FORM,
    DISPLAY_LIST,
    ATTR_LIST_ENTRY_ID,
    GET_LIST_ENTRY_TITLE,
    GET_LIST_ENTRY_ID,
    RETRIEVE_ITEM,
    RENDER_ITEM,
    TABLE_CLEAR,
    TABLE_USE_TABLE,
    TABLE_USE_KEY,
    TABLE_GET_ITEM,
    TABLE_SET_ITEM,
    TABLE_DELETE_ITEM,
    TABLE_GET_FIRST_KEY,
    TABLE_GET_NEXT_KEY,
    GET_SELECTED_ITEM,
    GET_SELECTED_ITEM_ID,
    ATTR_LIST_ENTRY_TYPE,
    GET_NUM_SELECTIONS,
    GET_FIRST_SELECTION,
    GET_NEXT_SELECTION,
    CHECK_ITEM_ID,
    UNCHECK_ITEM_ID,
    DELETE_ITEM_ID,
    ADD_ENTRY_TEXT,
    ADD_ENTRY_TAB,
    ADD_ENTRY_DATE_TIME,
    TABLE_GET_ITEM_VALUE,
    TABLE_SET_ITEM_VALUE,
    TABLE_ENCRYPT_TABLE;

    private final AtomDefinition def;

    LmAtom() {
        String atomName = "lm_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static LmAtom fromAtomNumber(int atomNumber) {
        for (LmAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static LmAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (LmAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
