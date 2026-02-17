package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum RichAtom implements DslAtom {
    MANAGE,
    EDIT_ACTION,
    ADD_URL,
    SET_COLOR_ITEM,
    BOOL_READ_ONLY,
    OPEN_FILE,
    TEXT_COLOR,
    FONT_JUSTIFY,
    BACKGROUND_COLOR,
    FONT_SIZE,
    FONT_ATTRIBUTE,
    WINDOW_COLOR,
    GET_TEXT_ALIGNMENT,
    GET_TEXT_STYLE,
    SET_STATIC,
    COLOR_DIALOG,
    VIEW_TEXT_COLOR,
    VIEW_TEXT_BACKGROUND_COLOR,
    VIEW_BACKGROUND_COLOR,
    FONT_SELECT_FACE,
    FONT_SELECT_SIZE,
    DROP_PICTURE,
    DROP_LINK,
    GET_TEXT_FONT_SIZE,
    GET_FONT,
    ALLOW_JUSTIFY,
    ALLOW_FONTSIZE,
    ALLOW_BACKCOLOR,
    ALLOW_FONTWINDOW,
    ALLOW_EMBEDS,
    ALLOW_TEXTFILE,
    IGNORE_TABS,
    ALLOW_ATTRIBUTES,
    IMAGE_MAX,
    DROP_BACK_PICTURE,
    DROP_TEXT_FILE,
    SCALE_IMAGES,
    INSERT_SMILIE,
    ENABLE_SMILIE,
    HTML_MODE;

    private final AtomDefinition def;

    RichAtom() {
        String atomName = "rich_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static RichAtom fromAtomNumber(int atomNumber) {
        for (RichAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static RichAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (RichAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
