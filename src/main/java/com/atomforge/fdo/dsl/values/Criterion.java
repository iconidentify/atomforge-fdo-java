package com.atomforge.fdo.dsl.values;

/**
 * FDO criterion values for act_set_criterion and act_do_action atoms.
 *
 * Specifies when an action should be triggered.
 *
 * <p>For criterion codes not in this enum, use {@link CriterionArg#of(int)} or
 * {@link CriterionArg#of(String)} to create a raw criterion argument.
 */
public enum Criterion {
    SELECT(1, "select"),
    CLOSE(2, "close"),
    GAIN_FOCUS(4, "gain_focus"),
    LOSE_FOCUS(5, "lose_focus"),
    CHANGE(7, "change"),
    DOUBLE_CLICK(8, "double_click"),
    KEY_PRESS(10, "key_press"),
    MOUSE_OVER(11, "mouse_over"),
    TIMER(18, "timer"),
    RIGHT_CLICK(20, "right_click"),
    DRAG_DROP(23, "drag_drop"),
    RESIZE(24, "resize"),
    SCROLL(25, "scroll");

    private final int code;
    private final String fdoName;

    Criterion(int code, String fdoName) {
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
     * Look up a Criterion by its binary code.
     * @param code The binary code
     * @return The matching Criterion, or null if not found
     */
    public static Criterion fromCode(int code) {
        for (Criterion crit : values()) {
            if (crit.code == code) {
                return crit;
            }
        }
        return null;
    }

    /**
     * Look up a Criterion by its FDO text name.
     * @param name The FDO name (e.g., "select", "gain_focus")
     * @return The matching Criterion, or null if not found
     */
    public static Criterion fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (Criterion crit : values()) {
            if (crit.fdoName.equals(lower)) {
                return crit;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "criterion_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        Criterion crit = fromCode(code);
        return crit != null ? crit.fdoName : String.format("criterion_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 1 (select) for unknown names
     */
    public static int codeFromName(String name) {
        Criterion crit = fromName(name);
        return crit != null ? crit.code : 1;
    }
}
