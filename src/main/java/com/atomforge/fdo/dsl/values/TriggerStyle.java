package com.atomforge.fdo.dsl.values;

/**
 * FDO trigger styles for mat_trigger_style atoms.
 *
 * Defines the visual appearance of trigger (button) objects.
 * Encoded as 2-byte big-endian integer.
 */
public enum TriggerStyle {
    DEFAULT(0, "default"),
    PLACE(1, "place"),
    RECTANGLE(2, "rectangle"),
    PICTURE(3, "picture"),
    FRAMED(4, "framed"),
    BOTTOM_TAB(5, "bottom_tab"),
    PLAIN_PICTURE(6, "plain_picture"),
    GROUP_STATE(7, "group_state");

    private final int code;
    private final String fdoName;

    TriggerStyle(int code, String fdoName) {
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
     * Look up a TriggerStyle by its binary code.
     * @param code The binary code (0-7)
     * @return The matching TriggerStyle, or null if not found
     */
    public static TriggerStyle fromCode(int code) {
        for (TriggerStyle style : values()) {
            if (style.code == code) {
                return style;
            }
        }
        return null;
    }

    /**
     * Look up a TriggerStyle by its FDO text name.
     * @param name The FDO name (e.g., "framed", "rectangle")
     * @return The matching TriggerStyle, or null if not found
     */
    public static TriggerStyle fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (TriggerStyle style : values()) {
            if (style.fdoName.equals(lower)) {
                return style;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "trigger_style_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        TriggerStyle style = fromCode(code);
        return style != null ? style.fdoName : String.format("trigger_style_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (default) for unknown names
     */
    public static int codeFromName(String name) {
        TriggerStyle style = fromName(name);
        return style != null ? style.code : 0;
    }
}
