package com.atomforge.fdo.dsl.values;

/**
 * FDO title position codes for mat_title_pos atoms.
 *
 * Specifies where the title label appears relative to an object.
 * Encoded as piped values: "orientation|justify" (e.g., "left|center").
 *
 * Byte format: bits 7-6 = orientation, bits 1-0 = justify
 */
public enum TitlePosition {
    LEFT_CENTER(0x00, TitleOrientation.LEFT, TitleJustify.CENTER),
    LEFT_TOP_OR_LEFT(0x01, TitleOrientation.LEFT, TitleJustify.TOP_OR_LEFT),
    LEFT_BOTTOM_OR_RIGHT(0x02, TitleOrientation.LEFT, TitleJustify.BOTTOM_OR_RIGHT),
    ABOVE_CENTER(0x40, TitleOrientation.ABOVE, TitleJustify.CENTER),
    ABOVE_TOP_OR_LEFT(0x41, TitleOrientation.ABOVE, TitleJustify.TOP_OR_LEFT),
    ABOVE_BOTTOM_OR_RIGHT(0x42, TitleOrientation.ABOVE, TitleJustify.BOTTOM_OR_RIGHT),
    RIGHT_CENTER(0x80, TitleOrientation.RIGHT, TitleJustify.CENTER),
    RIGHT_TOP_OR_LEFT(0x81, TitleOrientation.RIGHT, TitleJustify.TOP_OR_LEFT),
    RIGHT_BOTTOM_OR_RIGHT(0x82, TitleOrientation.RIGHT, TitleJustify.BOTTOM_OR_RIGHT),
    BELOW_CENTER(0xC0, TitleOrientation.BELOW, TitleJustify.CENTER),
    BELOW_TOP_OR_LEFT(0xC1, TitleOrientation.BELOW, TitleJustify.TOP_OR_LEFT),
    BELOW_BOTTOM_OR_RIGHT(0xC2, TitleOrientation.BELOW, TitleJustify.BOTTOM_OR_RIGHT);

    private final int code;
    private final TitleOrientation orientation;
    private final TitleJustify justify;

    TitlePosition(int code, TitleOrientation orientation, TitleJustify justify) {
        this.code = code;
        this.orientation = orientation;
        this.justify = justify;
    }

    /**
     * @return The binary code used in FDO format
     */
    public int code() {
        return code;
    }

    /**
     * @return The title orientation component
     */
    public TitleOrientation orientation() {
        return orientation;
    }

    /**
     * @return The title justify component
     */
    public TitleJustify justify() {
        return justify;
    }

    /**
     * @return The piped name used in FDO text format (e.g., "left|center")
     */
    public String fdoName() {
        return orientation.fdoName() + "|" + justify.fdoName();
    }

    /**
     * Look up a TitlePosition by its binary code.
     * @param code The binary code
     * @return The matching TitlePosition, or null if not found
     */
    public static TitlePosition fromCode(int code) {
        for (TitlePosition pos : values()) {
            if (pos.code == code) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Look up a TitlePosition by its piped FDO name.
     * @param name The FDO name (e.g., "left|center", "above|top_or_left")
     * @return The matching TitlePosition, or null if not found
     */
    public static TitlePosition fromName(String name) {
        if (name == null || !name.contains("|")) return null;
        String[] parts = name.toLowerCase().split("\\|");
        if (parts.length != 2) return null;

        TitleOrientation orient = TitleOrientation.fromName(parts[0].trim());
        TitleJustify just = TitleJustify.fromName(parts[1].trim());
        if (orient == null || just == null) return null;

        return of(orient, just);
    }

    /**
     * Create a TitlePosition from its components.
     * @param orientation The title orientation
     * @param justify The title justify
     * @return The matching TitlePosition, or null if not found
     */
    public static TitlePosition of(TitleOrientation orientation, TitleJustify justify) {
        int code = (orientation.ordinal() << 6) | justify.ordinal();
        return fromCode(code);
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or a computed piped format for unknown codes
     */
    public static String nameFromCode(int code) {
        TitlePosition pos = fromCode(code);
        if (pos != null) {
            return pos.fdoName();
        }
        // Decode the components for unknown codes
        int orient = (code >> 6) & 0x03;
        int justify = code & 0x03;
        String orientStr = switch (orient) {
            case 0 -> "left";
            case 1 -> "above";
            case 2 -> "right";
            case 3 -> "below";
            default -> "orient_" + orient;
        };
        String justStr = switch (justify) {
            case 0 -> "center";
            case 1 -> "top_or_left";
            case 2 -> "bottom_or_right";
            default -> "justify_" + justify;
        };
        return orientStr + "|" + justStr;
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (left|center) for unknown names
     */
    public static int codeFromName(String name) {
        TitlePosition pos = fromName(name);
        return pos != null ? pos.code : 0;
    }

    /**
     * Title orientation component.
     */
    public enum TitleOrientation {
        LEFT("left"),
        ABOVE("above"),
        RIGHT("right"),
        BELOW("below");

        private final String fdoName;

        TitleOrientation(String fdoName) {
            this.fdoName = fdoName;
        }

        public String fdoName() {
            return fdoName;
        }

        public static TitleOrientation fromName(String name) {
            if (name == null) return null;
            for (TitleOrientation o : values()) {
                if (o.fdoName.equalsIgnoreCase(name)) {
                    return o;
                }
            }
            return null;
        }
    }

    /**
     * Title justify component.
     */
    public enum TitleJustify {
        CENTER("center"),
        TOP_OR_LEFT("top_or_left"),
        BOTTOM_OR_RIGHT("bottom_or_right");

        private final String fdoName;

        TitleJustify(String fdoName) {
            this.fdoName = fdoName;
        }

        public String fdoName() {
            return fdoName;
        }

        public static TitleJustify fromName(String name) {
            if (name == null) return null;
            for (TitleJustify j : values()) {
                if (j.fdoName.equalsIgnoreCase(name)) {
                    return j;
                }
            }
            return null;
        }
    }
}
