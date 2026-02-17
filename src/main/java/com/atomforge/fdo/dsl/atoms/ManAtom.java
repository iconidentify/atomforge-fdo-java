package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ManAtom implements DslAtom {
    START_OBJECT,
    START_SIBLING,
    END_OBJECT,
    CLOSE,
    CLOSE_CHILDREN,
    DO_MAGIC_TOKEN_ARG,
    DO_MAGIC_RESPONSE_ID,
    SET_RESPONSE_ID,
    SET_CONTEXT_RESPONSE_ID,
    SET_CONTEXT_GLOBALID,
    SET_CONTEXT_RELATIVE,
    SET_CONTEXT_INDEX,
    CHANGE_CONTEXT_RELATIVE,
    CLEAR_RELATIVE,
    CLEAR_OBJECT,
    USE_DEFAULT_TITLE,
    UPDATE_DISPLAY,
    UPDATE_WOFF_END_STREAM,
    UPDATE_END_OBJECT,
    APPEND_DATA,
    REPLACE_DATA,
    PRESET_GID,
    PRESET_TITLE,
    PLACE_CURSOR,
    SET_DOMAIN_TYPE,
    SET_DOMAIN_INFO,
    RESPONSE_POP,
    CLOSE_UPDATE,
    END_CONTEXT,
    ITEM_GET,
    ITEM_SET,
    START_FIRST,
    DO_EDIT_MENU,
    LOGGING_COMMAND,
    GET_INDEX_BY_TITLE,
    START_ALPHA,
    START_LAST,
    INSERT_OBJECT_AFTER,
    CUT_OBJECT,
    COPY_OBJECT,
    PASTE_OBJECT,
    IS_RENDERED,
    PRESET_RELATIVE,
    INSERT_OBJECT_BEFORE,
    MAKE_FOCUS,
    GET_TOP_WINDOW,
    GET_FIRST_RESPONSE_ID,
    GET_NEXT_RESPONSE_ID,
    GET_RESPONSE_WINDOW,
    GET_REQUEST_WINDOW,
    IGNORE_RESPONSE,
    GET_FIRST_WINDOW,
    GET_NEXT_WINDOW,
    IS_RESPONSE_PENDING,
    IS_RESPONSE_IGNORED,
    GET_ATTRIBUTE,
    SET_ITEM_TYPE,
    SET_DEFAULT_TITLE,
    GET_CHILD_COUNT,
    CHECK_AND_SET_CONTEXT_RID,
    CLEAR_FILE_NAME,
    IS_WINDOW_ICONIC,
    POST_UPDATE_GID,
    END_DATA,
    UPDATE_FONTS,
    ENABLE_ONE_SHOT_TIMER,
    ENABLE_CONTINUOUS_TIMER,
    KILL_TIMER,
    FORCE_UPDATE,
    SET_EDIT_POSITION,
    SET_EDIT_POSITION_TO_END,
    PRESET_AUTHORING_FORM,
    ADD_DATE_TIME,
    START_TITLE,
    ADD_TITLE_TEXT,
    ADD_TITLE_TAB,
    ADD_TITLE_DATE_TIME,
    END_TITLE,
    PRESET_URL,
    GET_DROPPED_URL,
    FORCE_OLD_STYLE_DOD,
    PRESET_TAG,
    BUILD_FONT_LIST,
    SPELL_CHECK,
    OBJ_STACK_PUSH,
    OBJ_STACK_POP,
    DISPLAY_POPUP_MENU,
    WM_CLOSE,
    SET_APPEND_SECURE_DATA,
    APPEND_SECURE_DATA,
    START_IP_SESSION,
    END_IP_SESSION,
    BUILD_SAVEMAIL_MENU,
    BUILD_FAVORITES_MENU,
    GET_DISPLAY_CHARACTERISTICS,
    BUILD_SIGNATURES_MENU,
    SORT_ITEMS,
    ACCESSIBILITY_SETTING,
    TREECTRL_ACTION_COMMAND,
    SET_CONTEXT_FIRST_SELECTION,
    SET_CONTEXT_NEXT_SELECTION;

    private final AtomDefinition def;

    ManAtom() {
        String atomName = "man_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ManAtom fromAtomNumber(int atomNumber) {
        for (ManAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ManAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ManAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
