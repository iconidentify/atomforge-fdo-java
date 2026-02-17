package com.atomforge.fdo.dsl.values;

/**
 * FDO sort orders for mat_sort_order atoms.
 *
 * Specifies how list items should be sorted.
 */
public enum SortOrder {
    NORMAL(0, "normal"),
    REVERSE(1, "reverse"),
    ALPHABETICAL(2, "alphabetical");

    private final int code;
    private final String fdoName;

    SortOrder(int code, String fdoName) {
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
     * Look up a SortOrder by its binary code.
     * @param code The binary code (0-2)
     * @return The matching SortOrder, or null if not found
     */
    public static SortOrder fromCode(int code) {
        for (SortOrder order : values()) {
            if (order.code == code) {
                return order;
            }
        }
        return null;
    }

    /**
     * Look up a SortOrder by its FDO text name.
     * @param name The FDO name (e.g., "normal", "alphabetical")
     * @return The matching SortOrder, or null if not found
     */
    public static SortOrder fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (SortOrder order : values()) {
            if (order.fdoName.equals(lower)) {
                return order;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "sort_order_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        SortOrder order = fromCode(code);
        return order != null ? order.fdoName : String.format("sort_order_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (normal) for unknown names
     */
    public static int codeFromName(String name) {
        SortOrder order = fromName(name);
        return order != null ? order.code : 0;
    }
}
