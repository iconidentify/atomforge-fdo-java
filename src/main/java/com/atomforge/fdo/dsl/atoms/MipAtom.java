package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum MipAtom implements DslAtom {
    START_MESSAGE,
    HEADER_COMPLETE,
    END_MESSAGE,
    HEADER_ACCEPTED,
    MESSAGE_ACCEPTED,
    ERROR,
    ABORT,
    START_BLOCK,
    END_BLOCK,
    ADDRESSEE,
    SUBJECT,
    TEXT,
    DATA,
    MESSAGE_ID,
    AUTHOR,
    MESSAGE_DATE,
    MESSAGE_DATESTR,
    FORWARD,
    ATTR_ADDRESSEE_TYPE,
    ATTR_DATA_TYPE,
    ATTR_FILE_NAME,
    ATTR_FILE_SIZE,
    ATTR_FILE_TYPE,
    ATTR_FILE_CREATE_DATE,
    ATTR_MESSAGE_TYPE,
    BOOL_RETURN_RECEIPT,
    ATTR_LIST_TYPE,
    ADDRESSEE_COMMENT,
    ACTION_COMMAND,
    SET_CALLING_FORM_ID,
    END_OF_DATA,
    SCHEDULER_MODE,
    FLIP_IT,
    EDIT_OUTBOX,
    PRESET_FORM_TITLE,
    GET_ACTIVE_NAME_COUNT,
    IS_NAME_ACTIVE,
    SET_NAME_ACTIVE,
    CLEAR_ACTIVE_NAMES,
    SET_FORWARD_REF,
    ATTR_FOLDER,
    ABORT_ALL,
    PRESET_MESSAGE_ID,
    CHANGE_MESSAGE_STATUS,
    ATTR_ENCODING_FORMAT,
    BOOL_EVERYONE_INVISIBLE,
    REPLY_TO,
    DO_NOT_REPLY_TO,
    READ_MAIL,
    BOOL_WILL_ACK,
    BOOL_INET_QUOTING,
    START_ERRORS,
    END_ERRORS,
    DISPLAY_MAIL_FORM,
    DELETE_ONLINE_LISTS,
    ATTR_MESSAGE_SIZE,
    ERROR_CODE,
    FOLDER_NAME,
    FOLDER_PASSWORD,
    TRANSACTION_TYPE,
    BOOL_MORE_TO_COME,
    SENDER_ID,
    GET_NEXT_TYPED,
    GET_PREV_TYPED,
    SPELL_CHECK_SEND_NOW,
    SPELL_CHECK_SEND_LATER,
    ATTR_EMBED_OBJECT,
    EMAIL_ID_P1,
    EMAIL_ID_P2,
    EMAIL_ID_ALIAS,
    EXP_CLASS_ID,
    EXP_CLASS_NAME,
    FILTER_ACTIVE,
    FILTER_NAME,
    FILTER_CLASS_ID,
    FILTER_NUMBER,
    FOLDER_ID,
    STORAGE_CLASS_ID,
    STORAGE_CLASS_NAME,
    UID_VALIDITY,
    MAIL_COUNTS,
    VIEW_NAME,
    PROFILE,
    CONTINUE_CONTEXT;

    private final AtomDefinition def;

    MipAtom() {
        String atomName = "mip_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static MipAtom fromAtomNumber(int atomNumber) {
        for (MipAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static MipAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (MipAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
