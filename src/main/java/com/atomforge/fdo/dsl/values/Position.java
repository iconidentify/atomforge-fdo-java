package com.atomforge.fdo.dsl.values;

/**
 * FDO position codes for mat_position atoms.
 *
 * Window/object positioning options for UI elements.
 */
public enum Position {
    CASCADE(0, "cascade"),
    TOP_LEFT(1, "top_left"),
    TOP_CENTER(2, "top_center"),
    TOP_RIGHT(3, "top_right"),
    CENTER_LEFT(4, "center_left"),
    CENTER_CENTER(5, "center_center"),
    CENTER_RIGHT(6, "center_right"),
    BOTTOM_LEFT(7, "bottom_left"),
    BOTTOM_CENTER(8, "bottom_center"),
    BOTTOM_RIGHT(9, "bottom_right");

    private final int code;
    private final String fdoName;

    Position(int code, String fdoName) {
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
     * Look up a Position by its binary code.
     * @param code The binary code (0-10)
     * @return The matching Position, or null if not found
     */
    public static Position fromCode(int code) {
        for (Position pos : values()) {
            if (pos.code == code) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Look up a Position by its FDO text name.
     * @param name The FDO name (e.g., "cascade", "top_left")
     * @return The matching Position, or null if not found
     */
    public static Position fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (Position pos : values()) {
            if (pos.fdoName.equals(lower)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "position_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        Position pos = fromCode(code);
        return pos != null ? pos.fdoName : String.format("position_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (cascade) for unknown names
     */
    public static int codeFromName(String name) {
        Position pos = fromName(name);
        return pos != null ? pos.code : 0;
    }
}
