package com.atomforge.fdo.dsl.values;

/**
 * FDO font identifiers for mat_font_id atoms.
 *
 * Standard Windows font mappings used in FDO UI definitions.
 */
public enum FontId {
    ARIAL(0, "arial"),
    COURIER(1, "courier"),
    TIMES_ROMAN(2, "times_roman"),
    SYSTEM(3, "system"),
    FIXED_SYSTEM(4, "fixed_system"),
    MS_SERIF(5, "ms_serif"),
    MS_SANS_SERIF(6, "ms_sans_serif"),
    SMALL_FONTS(7, "small_fonts"),
    COURIER_NEW(8, "courier_new"),
    SCRIPT(9, "script"),
    MS_MINCHO(10, "ms_mincho"),
    MS_GOTHIC(11, "ms_gothic");

    private final int code;
    private final String fdoName;

    FontId(int code, String fdoName) {
        this.code = code;
        this.fdoName = fdoName;
    }

    /**
     * @return The binary code used in FDO format
     */
    public int code() {
        return code;
    }

    /**
     * @return The name used in FDO text format
     */
    public String fdoName() {
        return fdoName;
    }

    /**
     * Look up a FontId by its binary code.
     * @param code The binary code (0-8)
     * @return The matching FontId, or null if not found
     */
    public static FontId fromCode(int code) {
        for (FontId font : values()) {
            if (font.code == code) {
                return font;
            }
        }
        return null;
    }

    /**
     * Look up a FontId by its FDO text name.
     * @param name The FDO name (e.g., "arial", "courier")
     * @return The matching FontId, or null if not found
     */
    public static FontId fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        // Handle aliases
        if (lower.equals("fixedsys")) {
            return FIXED_SYSTEM;
        }
        for (FontId font : values()) {
            if (font.fdoName.equals(lower)) {
                return font;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "font_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        FontId font = fromCode(code);
        return font != null ? font.fdoName : String.format("font_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (arial) for unknown names
     */
    public static int codeFromName(String name) {
        FontId font = fromName(name);
        return font != null ? font.code : 0;
    }
}
