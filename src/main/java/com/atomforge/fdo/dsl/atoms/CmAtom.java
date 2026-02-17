package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum CmAtom implements DslAtom {
    MARK_TOOL_INVALID,
    SET_BYTE_COUNT,
    FORCE_OFF,
    INQUIRE_ADD_TOOL,
    INQUIRE,
    INQUIRE_ALL,
    CLEAR_LIST,
    LOAD_TOOL,
    KILL_TOOL,
    BOUNCE_TOOL,
    VERSION,
    CHECK_TOOL,
    CANCEL_JOB,
    MARK_PROTO_INVALID,
    MARK_ATOM_INVALID,
    MARK_TOKEN_INVALID,
    USE_PROTO,
    START_BLOCK,
    END_BLOCK,
    SET_CANCEL_RECORD,
    SET_TO_RESTART,
    CANCEL_HELD_STREAMS,
    CHECK_WID,
    GET_TID_FROM_WID,
    START_REQUIRED_TOOL_LIST,
    END_REQUIRED_TOOL_LIST,
    ADD_REQUIRED_TOOL,
    IS_TOOL_LOADED,
    DISK_SPACE_NEEDED,
    SET_SPLASH_RECORD,
    UPDATE_NOTIFY_TOKEN,
    TOD_NAME,
    SET_REQUEST_RECORD,
    MARK_WIDGET_INVALID,
    ACCEPT_UPDATE,
    REJECT_UPDATE,
    GET_DISK_SPACE_REQUIRED,
    GET_TIME_ESTIMATE,
    IS_RESTART_NECESSARY,
    SHOW_TOOLS,
    INQUIRE_CHANGED,
    TRIGGER_TOD,
    TOOL_VERIFY_SET_TID,
    TOOL_VERIFY,
    SET_CLEANUP_RECORD,
    HOST_LIB_INDEX,
    LIB_START_DATE,
    LIB_END_DATE,
    VERSION_INFO,
    MISSING_PROTOCOL,
    MISSING_ATOM,
    MISSING_TOOL,
    MISSING_TOKEN,
    JOB_ID,
    PACKET_NUM,
    TOOL_ID,
    WIDGET_ID,
    HOST_UPDATE_ACK,
    TB_SET_NAME,
    TB_TEST_VER,
    TB_GET_VER,
    TB_GET_PATH,
    TB_TEST_NAME_LOADED,
    TB_CLEAR_NAME,
    TB_TEST_NAME_EXISTS,
    TB_GET_WINDOWS_DIR,
    TB_WIN_GETREGISTRYKEY,
    TB_GET_PHYSICAL_MEMSIZE;

    private final AtomDefinition def;

    CmAtom() {
        String atomName = "cm_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static CmAtom fromAtomNumber(int atomNumber) {
        for (CmAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static CmAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (CmAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
