package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum AsyncAtom implements DslAtom {
    EXIT,
    PASSWORD,
    GUEST_PASSWORD,
    EXIT_DAMNIT,
    ONLINE,
    OFFLINE,
    ERROR_BOX,
    ALERT,
    DISPLAY_NETNEWS,
    GO_NETNEWS,
    MOREINFO_NETNEWS,
    PLAYSOUND,
    EXIT_AUX,
    EXEC_HELP,
    EXEC_CONTEXT_HELP,
    PLAY_SOUND_DAMMIT,
    EXEC_HELP_FILE,
    FORCE_OFF,
    SEND_CLIENTSTATUS,
    GET_STAT_COUNT,
    EXTRACT_STATS,
    STAT_COLLECTION_STATE,
    CLEAR_STATS,
    STAT_RECORD,
    GET_ALERT_RESULT,
    EXEC_APP,
    SCREEN_NAME_CHANGED,
    IS_KNOWN_SUBACCOUNT,
    DUMP_DIAG,
    GET_SCREEN_NAME,
    SIGN_ON,
    ALERT_START,
    ALERT_ADD_TEXT,
    ALERT_ADD_DATE_TIME,
    ALERT_END,
    INVOKE_TIMEZONE_PREF,
    INVOKE_LANGUAGE_PREF,
    SET_SCREEN_NAME,
    AUTO_LAUNCH,
    LAUNCHER_NAME,
    IS_CLIENT_32BIT,
    DISPLAY_ERRORS,
    IS_GUEST,
    RELOGON_INIT,
    RELOGON,
    STORENAME,
    STOREPASSWORD,
    SIGNOFF,
    IS_CURRENT_SCREENNAME,
    LOGOUT,
    CHECK_AND_INVOKE_DRIVEWAY,
    RECORD_ERROR,
    SYSTEM_USAGE,
    REPLACE_PREF,
    BOOL_DIAG_ON,
    ALLOW_SWITCH_SCREEN_NAMES,
    INSTALL_SOUND,
    VOICE_RECOGNITION,
    GET_OS;

    private final AtomDefinition def;

    AsyncAtom() {
        String atomName = "async_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static AsyncAtom fromAtomNumber(int atomNumber) {
        for (AsyncAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static AsyncAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (AsyncAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
