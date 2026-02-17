package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ImageAtom implements DslAtom {
    SET_FILE_NAME,
    ATTR_ROTATE,
    FLIP_HORIZONTAL,
    FLIP_VERTICAL,
    INVERT,
    ATTR_SHARPNESS,
    ATTR_BRIGHTNESS,
    ATTR_CONTRAST,
    ATTR_IMAGE_TYPE,
    ATTR_USE_SUGGESTED_SIZE,
    ATTR_ASPECT_RATIO,
    ATTR_DYNAMIC_DISPLAY,
    GET_ATTR,
    INVOKE_EDITOR,
    MAKE_GRAY_SCALE,
    REVERT,
    SET_TO_ORGINAL_SIZE,
    CANCEL_MODAL,
    ATTR_EXTRACTION_FORMAT,
    ATTR_COMP_QUALITY,
    CROP_IMAGE,
    ATTR_HOTSPOT,
    ATTR_HOTSPOT_ACTION_ID,
    DELETE_HOTSPOT,
    ATTR_HOTSPOT_GRID,
    ATTR_IMAGE_CROPABLE,
    ATTR_DRAW_HOTSPOT_GRID,
    ATTR_HOTSPOT_DEF_ACTION,
    ATTR_SHOW_HOTSPOT,
    ATTR_FIT_TO_VIEW,
    ATTR_BACKGROUND_BLEND,
    ATTR_HOTSPOT_TYPE,
    ATTR_CUSTOM_HILIGHT,
    ATTR_HOTSPOT_OFFSET,
    ATTR_HILIGHT_TYPE,
    ATTR_CUSTOM_OFFSET,
    ATTR_FILE_ID,
    ATTR_THUMBNAIL,
    ATTR_SAVE_ORIG_SIZE,
    OPEN_FILE_RELATIVE,
    DB_ID,
    ATTR_SCALE_DISPLAY,
    ATTR_SCALE_BEST_FIT;

    private final AtomDefinition def;

    ImageAtom() {
        String atomName = "image_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ImageAtom fromAtomNumber(int atomNumber) {
        for (ImageAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ImageAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ImageAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
