package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum RadioAtom implements DslAtom {
    CODEC,
    ATR_FILE_NAME,
    ATR_STREAM_NAME,
    ATR_STREAM_DESCR,
    ATR_WAVE_FILE_NAME,
    ATR_STREAM_SOURCE,
    ATR_STREAM_TARGET,
    START_STREAM,
    STOP_STREAM,
    GET_ERROR_TEXT,
    GET_STATUS_TEXT,
    ERROR_ACTION,
    STATUS_ACTION,
    SPLASH_ARTID,
    RADMIC_STATUS,
    START_PLAY,
    STOP_PLAY,
    START_ACTION,
    READY_GO_LIVE_ACTION,
    GO_LIVE_ACTION,
    GO_LIVE_REQUEST,
    ATR_CHANNEL,
    GO_LIVE_STATUS,
    READY_GO_LIVE_STATUS,
    PLAY_NAME_ACTION,
    PLAY_NAME_TEXT,
    PLAY_DESC_ACTION,
    PLAY_DESC_TEXT,
    PLAY_CAPTION_ACTION,
    PLAY_CAPTION_TEXT,
    ATR_CAPTION,
    DROPPOINT_OBJECT,
    FORM_IS_CLOSING,
    PAUSE_PLAY,
    RESUME_PLAY;

    private final AtomDefinition def;

    RadioAtom() {
        String atomName = "radio_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static RadioAtom fromAtomNumber(int atomNumber) {
        for (RadioAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static RadioAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (RadioAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
