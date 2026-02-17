package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum ChartAtom implements DslAtom {
    TYPE,
    XMAX,
    YMAX,
    XMIN,
    YMIN,
    SCALE,
    SET_STRIPE_TOP,
    BOTTOM_LABEL,
    LEFT_LABEL,
    TITLE,
    CHECKPOINT,
    REQUEST_INTERVAL,
    START_DATASET,
    END_DATASET,
    SET_DATASET_CONTEXT,
    END_DATASET_CONTEXT,
    SET_DATASET_SHAPE,
    RIGHT_LABEL,
    SET_DATASET_COLOR,
    SET_BAR_INIT_VALUE,
    SET_POINT_CONTEXT,
    SET_POINT_VALUE,
    SET_CONSTANT,
    SET_XVALUE,
    SET_STRIPE_BOTTOM,
    SET_YLEGEND_CONTEXT,
    SET_3D_X_ROTATION,
    SET_3D_Y_ROTATION,
    SET_MARKER_SIZE,
    SET_DATASET_BKCOLOR,
    SET_POINT_TYPE,
    SET_DECIMALS,
    SET_CONSTANT_BKCOLOR,
    SET_CONSTANT_COLOR,
    SET_CONSTANT_WIDTH,
    SET_CONSTANT_LABEL,
    SET_PATTERN_SCHEME,
    SET_LINE_BKCOLOR,
    SET_LINE_COLOR,
    SET_LINE_STYLE,
    SET_LINE_WIDTH,
    SET_CHART_BKCOLOR,
    SET_BKCOLOR,
    TITLE_FONT_SIS,
    RIGHT_FONT_SIS,
    LEFT_FONT_SIS,
    BOTTOM_FONT_SIS,
    ADD_DATASET_LEGEND,
    BOOL_STACKED,
    BOOL_HORIZ_GRID,
    BOOL_VERT_GRID,
    BOOL_HORIZ_BAR,
    BOOL_SHOW_TOOLS,
    BOOL_3D_VIEW,
    BOOL_SHOW_PALETTE,
    BOOL_SHOW_PATTERNS,
    BOOL_SHOW_MENU,
    BOOL_SHOW_LEGEND,
    BOOL_BARS_TOGETHER,
    BOOL_SHOW_POINTS,
    BOOL_SHOW_ZERO,
    BOOL_EACH_BAR,
    BOOL_CLUSTER,
    BOOL_SHOWDATA,
    BOOL_DLGGRAY,
    ADD_YLEGEND_TEXT,
    ADD_XLEGEND_TEXT,
    SET_CONSTANT_CONTEXT,
    SET_STRIPE_COLOR,
    SET_STRIPE_CONTEXT,
    START_STRIPE,
    END_STRIPE,
    ADD_KEY_LEGEND_TEXT,
    SET_YLEGEND_GAP,
    START_CONSTANT,
    END_CONSTANT,
    SET_CONSTANT_STYLE,
    GET_ATTRIBUTE,
    SET_ATOM_DECIMAL_PLACES,
    XLEGEND_FONT_SIS,
    YLEGEND_FONT_SIS,
    CONSTANT_FONT_SIS,
    LEGEND_FONT_SIS,
    SET_RESPONSE_TOKEN,
    ATR_NUM_DATASETS,
    DATASET_CHART_TYPE,
    SET_CLOSE_TOKEN,
    SET_CLOSE_ARG,
    SET_TOOLS,
    SET_GALLERY,
    SET_PATTERN,
    SET_TOOLBAR_POS,
    SET_LEGEND_POS,
    SET_DATASET_LEGEND_POS,
    BOOL_SHOW_DS_LEGEND,
    MAC_SET_ATTR,
    BOOL_LEGEND_2LEVEL,
    BOOL_LEGEND_VERTICAL,
    MANAGE,
    SET_Y_INC_GAP,
    SET_MARGINS,
    ADD_DATE_LEGEND_TEXT;

    private final AtomDefinition def;

    ChartAtom() {
        String atomName = "chart_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static ChartAtom fromAtomNumber(int atomNumber) {
        for (ChartAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static ChartAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ChartAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
