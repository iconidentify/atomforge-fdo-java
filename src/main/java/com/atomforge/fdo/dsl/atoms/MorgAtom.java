package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum MorgAtom implements DslAtom {
    PROMPT_CREATE_FOLDER,
    DELETE_CURRENT_OBJECT,
    VIEW_PATH,
    VIEW_ID,
    OBJ_COUNT_CHILDREN,
    VIEW_INCLUDE_ROOT,
    PROMPT_RENAME_OBJECT,
    VIEW_SELECT_OBJECT,
    VIEW_SET_TO_SPAWN_PATH,
    OBJ_DESTROY,
    MANAGE,
    VIEW_HIGHLIGHT_FULL_LINE,
    VIEW_ALLOW_MULTI_SELECT,
    VIEW_SEARCH,
    OBJ_DESTROY_CHILDREN,
    OBJ_SET_CONTEXT,
    OBJ_END_CONTEXT,
    OBJ_START_OBJ,
    OBJ_END_OBJ,
    ATR_TITLE,
    ATR_URL,
    FLAG_OPEN,
    OBJ_START_FOLDER,
    OBJ_START_BOOKMARK,
    VIEW_HAS_MULT_SELECTIONS,
    OBJ_SET_CONTEXT_SELECTION,
    OBJ_CAN_DELETE,
    OBJ_CAN_RENAME,
    OBJ_GET_CLASS,
    JUMP_BOOKMARK,
    ADD_BOOKMARK,
    SET_BOOKMARK_URL,
    MODIFY_BOOKMARK,
    EDIT_BOOKMARK,
    FINISH_JUMP_BOOKMARK,
    SET_BOOKMARK_CLOSE_FORM,
    VIEW_ALLOW_DELETE_KEY,
    PFC_COMPRESS_DATABASE,
    JUMP_URL,
    PFC_CANCEL_COMPRESSION,
    PFC_CHECK_DB,
    PFC_CHECK_FRAGMENT,
    OBJ_CAN_CREATE,
    ACT_ON_SELECTED,
    SET_TAB_LIST,
    OBJ_GET_NAME,
    OBJ_SET_CONTEXT_BY_INDEX,
    SAVE_FOLDER,
    VIEW_HIDE_FOLDER,
    VIEW_SHOW_FOLDER,
    SET_SELECTION,
    SWAP_SCREENNAME,
    REPLACE_SCREENNAME,
    ADD_FOLDER,
    SAVE,
    RESTORE,
    SORT_ITEMS,
    OBJ_TEST_FLAG;

    private final AtomDefinition def;

    MorgAtom() {
        String atomName = "morg_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static MorgAtom fromAtomNumber(int atomNumber) {
        for (MorgAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static MorgAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (MorgAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
