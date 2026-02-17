package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum HfsAtom implements DslAtom {
    ATTR_FIELD_MAPPING,
    CMD_FORM_GID,
    CMD_FORM_NAME,
    ATTR_PRESET_GLOBAL_ID,
    ATTR_STYLE_ID,
    ATTR_CLUSTER_ID,
    ATTR_FLAGS,
    CMD_FDO,
    CMD_TAG,
    CMD_DATABASE_ID,
    CMD_RESULT_CODE,
    CMD_SAVE_AS,
    ATTR_START_PRE_STREAM,
    ATTR_END_PRE_STREAM,
    ATTR_START_IN_STREAM,
    ATTR_END_IN_STREAM,
    ATTR_START_POST_STREAM,
    ATTR_END_POST_STREAM,
    CMD_START_FORM_DATA,
    CMD_END_FORM_DATA,
    CMD_START_SAVE_RESULT,
    CMD_END_SAVE_RESULT,
    CMD_START_GET_FDO,
    CMD_END_GET_FDO,
    CMD_START_INSTALL_RESULT,
    CMD_END_INSTALL_RESULT,
    CMD_RESULT_MESSAGE,
    CMD_RESPONSE_ID,
    ATTR_VARIABLE_MAPPING,
    ATTR_DISPATCH_MAPPING,
    ATTR_CHECKBOX_MAPPING,
    ATTR_DATABASE_TYPE,
    CMD_REFERENCE_ID,
    CMD_TEMPLATE_NAME,
    ATTR_OBJECT_COMMENT,
    ATTR_OBJECT_NAME,
    ATTR_FDO_COMMENT,
    ATTR_PLUS_GROUP_NUMBER,
    ATTR_PLUS_GROUP_TYPE,
    ATTR_LAYER_NAME,
    CMD_READ_ONLY_FORM,
    ATTR_SERVER_NAME,
    CMD_ACCESS_RIGHTS,
    ATTR_END_OBJECT,
    ATTR_OBJECT_FLAGS,
    CMD_COMMAND,
    CMD_POSITION,
    CMD_PLATFORM,
    CMD_VIEW_RULE,
    CMD_LANGUAGE,
    CMD_START_RESULT,
    CMD_END_RESULT,
    CMD_PROCESS_MODE,
    CMD_START_ATOMS,
    CMD_ATOMS,
    CMD_END_ATOMS,
    ATTR_START_RAW_DISPLAY,
    ATTR_END_RAW_DISPLAY,
    CMD_ASPP_ID;

    private final AtomDefinition def;

    HfsAtom() {
        String atomName = "hfs_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static HfsAtom fromAtomNumber(int atomNumber) {
        for (HfsAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static HfsAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (HfsAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
