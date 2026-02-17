package com.atomforge.fdo.dsl.values;

/**
 * FDO font styles for mat_font_style atoms.
 *
 * Font style flags that can be combined for text rendering.
 */
public enum FontStyle {
    NORMAL(0, "normal"),
    BOLD(1, "bold"),
    ITALIC(2, "italic"),
    BOLD_ITALIC(3, "bold_italic"),
    UNDERLINE(4, "underline");

    private final int code;
    private final String fdoName;

    FontStyle(int code, String fdoName) {
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
     * Look up a FontStyle by its binary code.
     * @param code The binary code (0-4)
     * @return The matching FontStyle, or null if not found
     */
    public static FontStyle fromCode(int code) {
        for (FontStyle style : values()) {
            if (style.code == code) {
                return style;
            }
        }
        return null;
    }

    /**
     * Look up a FontStyle by its FDO text name.
     * @param name The FDO name (e.g., "bold", "italic")
     * @return The matching FontStyle, or null if not found
     */
    public static FontStyle fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (FontStyle style : values()) {
            if (style.fdoName.equals(lower)) {
                return style;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "style_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        FontStyle style = fromCode(code);
        return style != null ? style.fdoName : String.format("style_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (normal) for unknown names
     */
    public static int codeFromName(String name) {
        FontStyle style = fromName(name);
        return style != null ? style.code : 0;
    }
}
