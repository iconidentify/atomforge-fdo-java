package com.atomforge.fdo.dsl.values;

/**
 * FDO data types for de_data_type atoms.
 *
 * Specifies the type of data being extracted or sent.
 */
public enum DataType {
    DEFAULT(0, "default"),
    TEXT(1, "text"),
    VAR(2, "var"),
    BOOLEAN(3, "boolean"),
    GLOBAL_ID(4, "global_id"),
    NUMBER(5, "number"),
    PROPERTY(6, "property"),
    CONSTANT(7, "constant"),
    VARIABLE(8, "variable"),
    SELECTION(9, "selection"),
    GLOBAL_VAR(10, "global_var"),
    LOCAL_VAR(11, "local_var"),
    ITEM(12, "item"),
    RELATIVE_ID(13, "relative_id");

    private final int code;
    private final String fdoName;

    DataType(int code, String fdoName) {
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
     * Look up a DataType by its binary code.
     * @param code The binary code (0-12)
     * @return The matching DataType, or null if not found
     */
    public static DataType fromCode(int code) {
        for (DataType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * Look up a DataType by its FDO text name.
     * @param name The FDO name (e.g., "text", "global_id")
     * @return The matching DataType, or null if not found
     */
    public static DataType fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (DataType type : values()) {
            if (type.fdoName.equals(lower)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "data_type_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        DataType type = fromCode(code);
        return type != null ? type.fdoName : String.format("data_type_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (default) for unknown names
     */
    public static int codeFromName(String name) {
        DataType type = fromName(name);
        return type != null ? type.code : 0;
    }
}
