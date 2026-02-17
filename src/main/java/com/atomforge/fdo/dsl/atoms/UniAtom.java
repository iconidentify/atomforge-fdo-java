package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum UniAtom implements DslAtom {
    VOID,
    START_STREAM,
    END_STREAM,
    ABORT_STREAM,
    START_LARGE_ATOM,
    LARGE_ATOM_SEGMENT,
    END_LARGE_ATOM,
    SYNC_SKIP,
    START_LOOP,
    END_LOOP,
    USE_LAST_ATOM_STRING,
    USE_LAST_ATOM_VALUE,
    SAVE_RESULT,
    DATA,
    WAIT_ON,
    WAIT_OFF,
    START_STREAM_WAIT_ON,
    WAIT_OFF_END_STREAM,
    INVOKE_NO_CONTEXT,
    INVOKE_LOCAL,
    GET_RESULT,
    NEXT_ATOM_TYPED,
    START_TYPED_DATA,
    END_TYPED_DATA,
    FORCE_PROCESSING,
    SET_COMMAND_SET,
    WAIT_CLEAR,
    CHANGE_STREAM_ID,
    DIAGNOSTIC_MSG,
    HOLD,
    INVOKE_LOCAL_PRESERVE,
    INVOKE_LOCAL_LATER,
    CONVERT_LAST_ATOM_STRING,
    BREAK,
    SINGLE_STEP,
    CONVERT_LAST_ATOM_DATA,
    GET_FIRST_STREAM,
    GET_NEXT_STREAM,
    GET_STREAM_WINDOW,
    CANCEL_ACTION,
    GET_CURRENT_STREAM_ID,
    SET_DATA_LENGTH,
    USE_LAST_ATOM_DATA,
    SET_WATCHDOG_INTERVAL,
    UDO_COMPLETE,
    TEST_UPDATE,
    INSERT_STREAM,
    NEXT_ATOM_MIXED_DATA,
    START_MIXED_DATA_MODE,
    END_MIXED_DATA_MODE,
    TRANSACTION_ID,
    RESULT_CODE,
    COMMAND,
    GET_FROM_STREAM_REG,
    SAVE_TO_STREAM_REG,
    RESET_STREAM_REGS,
    STRING_TO_GID;

    private final AtomDefinition def;

    UniAtom() {
        String atomName = "uni_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static UniAtom fromAtomNumber(int atomNumber) {
        for (UniAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static UniAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (UniAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
