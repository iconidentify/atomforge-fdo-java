package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum FmAtom implements DslAtom {
    START,
    END,
    ITEM_TYPE,
    ITEM_SET,
    ITEM_GET,
    BROADCAST,
    HANDLE_ERROR,
    ADD_FILE_TYPE,
    DELETE_FILE_TYPE,
    ADD_FILE_TYPE_MASK,
    DELETE_FILE_TYPE_MASK,
    REGISTER,
    UNREGISTER,
    ADD_TYPE_TO_GROUP,
    DELETE_TYPE_FROM_GROUP,
    FIND_FILE_TYPE,
    DIALOG_GET_FILE,
    DIALOG_PUT_FILE,
    DIALOG_GET_TYPE,
    DIALOG_PRINT,
    CREATE_FILE,
    OPEN_FILE,
    DELETE_FILE,
    RENAME_FILE,
    CHECK_DISK_SPACE,
    CLOSE_FILE,
    APPEND_DATA,
    POSITION_FILE,
    POSITION_EOF,
    SEND_CONTENTS_TO_BUFFER,
    DUMP,
    GET_FIRST_FILE,
    GET_NEXT_FILE,
    INI_READ_DATA,
    INI_WRITE_DATA,
    SET_RELATIVE_PATH,
    READ_FILE,
    UNREGISTER_LIST,
    FLUSH_FILE,
    INI_GET_SECTION,
    INI_GET_NEXT_LINE,
    COPY_FILE,
    KEYWORD_OKAY,
    INI_DELETE_FILE,
    DIALOG_INIT_GET,
    DIALOG_END_GET,
    DIALOG_CANCEL_GET,
    EDIT_ATTACHMENT_COMMAND;

    private final AtomDefinition def;

    FmAtom() {
        String atomName = "fm_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static FmAtom fromAtomNumber(int atomNumber) {
        for (FmAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static FmAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (FmAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
