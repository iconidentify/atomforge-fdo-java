package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum VrmAtom implements DslAtom {
    START_FORM,
    SEND_FORM,
    SAVE_FORM,
    ACTION_COMMAND,
    START_HTML_FORM,
    FORM_UP,
    HTML_GET_WIZ,
    HTML_START_WIZ,
    HTML_FINISH_WIZ,
    AM_GET_SPAWN_PATH,
    HTML_GET_SPAWN_PATH,
    HTML_GET_SPAWN_FILE,
    HTML_FORM_UP,
    AAT_ART_START_RESULT,
    AAT_ART_END_RESULT,
    AAT_ART_GID,
    AAT_ART_NAME,
    AAT_ART_RETURN_CODE,
    AAT_ART_IMAGE_ID,
    AAT_ART_SIZE_X,
    AAT_ART_SIZE_Y,
    AAT_ART_LENGTH,
    AAT_ART_MODIFIED_BY,
    AAT_ART_MODIFIED_DATE,
    AAT_ART_FORMAT,
    AAT_ART_TYPE,
    AAT_ART_PLUS_GROUP,
    AAT_ART_DEST_GID,
    AAT_ART_TRIGGER_FORM_ID,
    AAT_ART_EXIT_FREE_PAY_CURTAIN,
    AAT_ART_REPOSITORY,
    SET_CONTEXT,
    AAT_QUERY_START_RESULT,
    AAT_QUERY_END_RESULT,
    AAT_ART_EXP_DATE,
    AAT_SET_OBJ_CONTEXT,
    AAT_START_RECORD,
    AAT_END_RECORD,
    AAT_ART_OWNERSHIP,
    GENERIC_QUERY,
    HTML_PRESET_MINIICON,
    HTML_PRESET_EDITFORM,
    CHOOSE_ART_ID,
    BOOL_OBJECT_NEEDS_UPDATE,
    FINALIZE_OBJECTS,
    START_FORM_INFO,
    HTML_SET_DOMAIN,
    EXTRACT_RESULT,
    EXTRACT_SAVED_RESULT,
    RAINMAN_HOST_MIN,
    START_RAINMAN,
    END_RAINMAN,
    RAINMAN_ERROR_CODE,
    RAINMAN_DATA,
    FDO_DATABLOCK;

    private final AtomDefinition def;

    VrmAtom() {
        String atomName = "vrm_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static VrmAtom fromAtomNumber(int atomNumber) {
        for (VrmAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static VrmAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (VrmAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
