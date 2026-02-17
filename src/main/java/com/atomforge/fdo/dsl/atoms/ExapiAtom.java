package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ExapiAtom implements DslAtom {
    LIST_START_CONTEXT,
    LIST_SET_STREAM,
    LIST_ATTR_SET_STYLE,
    LIST_ATTR_SET_TITLE,
    LIST_ATTR_SET_GID,
    LIST_ITEM_START,
    LIST_ITEM_SET_SREG,
    LIST_ITEM_SET_STR,
    LIST_ITEM_SET_NREG,
    LIST_ITEM_SET_NUM,
    LIST_ITEM_SET_FREG,
    LIST_ITEM_SET_FLAG,
    LIST_ITEM_SET_STREAM,
    LIST_ITEM_SET_ADDRESS,
    LIST_ITEM_SET_PORT,
    LIST_ITEM_END,
    LIST_END_CONTEXT,
    MANAGE,
    VERSION_VERIFY,
    LIST_CLEAR,
    LIST_ITEM_DELETE,
    IS_VALID_VERSION,
    ENTER_ANTEROOM,
    LEAVE_ANTEROOM,
    SET_SELECTION_ITEM,
    GAIN_FOCUS,
    GET_SERIAL_INFO,
    SET_SELECTION_LIST,
    GET_INSTANCE_INFO,
    GET_PARTICIPANT_LIST,
    GET_PARTICIPANT_INFO,
    BEGIN_VERSIONING,
    COMPONENT_BEGIN,
    COMPONENT_FILE_ID,
    COMPONENT_FILE_SIZE,
    COMPONENT_FILE_DATE,
    COMPONENT_FILE_OFFSET,
    COMPONENT_FILE_LIBRARY,
    COMPONENT_SYSFILE,
    COMPONENT_SYSVER,
    COMPONENT_END,
    END_VERSIONING,
    DELETE_CONTEXT,
    BEGIN_INSTALL,
    SET_DEFAULT_FOLDER_NAME,
    SET_REQUIRED_SPACE,
    SET_CONFIG_STYLE,
    SET_PROGRAM_NAME,
    SET_APP_NAME,
    END_INSTALL,
    GET_VERSION_INFO,
    LAUNCH_APPLICATION,
    COMPONENT_TOTAL_SIZE,
    SET_SETUP_APP_NAME,
    COMPONENT_SETUP_APP,
    COMPONENT_SET_FLAGS,
    SET_FLAGS,
    COMPONENT_SET_ID,
    MOVE_SHARED_DIR,
    SET_CURRENT_ADDRESS,
    UNINSTALL_APP,
    SET_UNINSTALLER_NAME,
    APP_TERMINATE,
    SET_SESSION_ID,
    SET_SESSION_FLAGS,
    SET_PLATFORM,
    PERFORM_VERSIONING,
    SET_MESSAGE_HANDLER,
    SET_IDLE_INTERVAL,
    IDLE,
    GET_VER_INFO_WREPLYTOKEN,
    SET_REPLY_TOKEN;

    private final AtomDefinition def;

    ExapiAtom() {
        String atomName = "exapi_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ExapiAtom fromAtomNumber(int atomNumber) {
        for (ExapiAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ExapiAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ExapiAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
