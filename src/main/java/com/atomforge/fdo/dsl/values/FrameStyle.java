package com.atomforge.fdo.dsl.values;

/**
 * FDO frame styles for mat_frame_style atoms.
 *
 * Defines the visual border/frame appearance of objects.
 * Encoded as 2-byte big-endian integer.
 */
public enum FrameStyle {
    NONE(0, "none"),
    SINGLE_LINE_POP_OUT(1, "single_line_pop_out"),
    SINGLE_LINE_POP_IN(2, "single_line_pop_in"),
    POP_IN(3, "pop_in"),
    POP_OUT(4, "pop_out"),
    DOUBLE_LINE(5, "double_line"),
    SHADOW(6, "shadow"),
    HIGHLIGHT(7, "highlight");

    private final int code;
    private final String fdoName;

    FrameStyle(int code, String fdoName) {
        this.code = code;
        this.fdoName = fdoName;
    }

    /**
     * @return The binary code used in FDO format (2-byte big-endian)
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
     * Look up a FrameStyle by its binary code.
     * @param code The binary code (0-7)
     * @return The matching FrameStyle, or null if not found
     */
    public static FrameStyle fromCode(int code) {
        for (FrameStyle style : values()) {
            if (style.code == code) {
                return style;
            }
        }
        return null;
    }

    /**
     * Look up a FrameStyle by its FDO text name.
     * @param name The FDO name (e.g., "pop_in", "shadow")
     * @return The matching FrameStyle, or null if not found
     */
    public static FrameStyle fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (FrameStyle style : values()) {
            if (style.fdoName.equals(lower)) {
                return style;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "frame_style_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        FrameStyle style = fromCode(code);
        return style != null ? style.fdoName : String.format("frame_style_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (none) for unknown names
     */
    public static int codeFromName(String name) {
        FrameStyle style = fromName(name);
        return style != null ? style.code : 0;
    }
}
