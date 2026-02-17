package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ChatAtom implements DslAtom {
    ROOM_OPEN,
    ADD_USER,
    END_OBJECT,
    REMOVE_USER,
    SHOW_ARRIVAL,
    MESSAGE,
    REFRESH_PREF,
    HOST,
    NAME_VIEW,
    NEW_ROOM_OPEN,
    SCREEN_NAME,
    NEW_MESSAGE,
    USER_ENTER_THRU_LIST,
    USER_EXIT,
    LOG_ROOM,
    LINK_VIEW_TO_LIST,
    HOST_MESSAGE,
    LINK_COUNT_TO_LIST,
    SET_VIEW_LOG_PRIORITY,
    LOG_PATH_RID,
    CLOSE_ROOM,
    BOOL_ANNOUNCE,
    IS_CHAT_VIEW,
    SCREEN_NAME_COLOR,
    SCREEN_NAME_BACK_COLOR,
    SCREEN_NAME_FONT_SIZE,
    ENUM_ROOMS_IN_LIST,
    IS_NEW_CHAT_ROOM,
    IS_ROOM_BEING_LOGGED,
    GET_LOG_FILE_NAME,
    SET_USERS_LIST_ICON,
    USER_ENTER,
    START_MESSAGE,
    ADD_MESSAGE_TEXT,
    ADD_MESSAGE_DATE_TIME,
    END_MESSAGE;

    private final AtomDefinition def;

    ChatAtom() {
        String atomName = "chat_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ChatAtom fromAtomNumber(int atomNumber) {
        for (ChatAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ChatAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ChatAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
