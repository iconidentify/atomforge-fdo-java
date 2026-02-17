package com.atomforge.fdo.dsl.values;

/**
 * FDO object types for man_objstart atoms.
 *
 * Object types define the kind of UI element being created in FDO streams.
 * Codes 0-14 are basic types, 15-28 are extended types.
 */
public enum ObjectType {
    // Basic object types (0-14)
    ORG_GROUP(0, "org_group"),
    IND_GROUP(1, "ind_group"),
    DMS_LIST(2, "dms_list"),
    SMS_LIST(3, "sms_list"),
    DSS_LIST(4, "dss_list"),
    SSS_LIST(5, "sss_list"),
    TRIGGER(6, "trigger"),
    ORNAMENT(7, "ornament"),
    VIEW(8, "view"),
    EDIT_VIEW(9, "edit_view"),
    BOOLEAN(10, "boolean"),
    SELECT_BOOLEAN(11, "select_boolean"),
    RANGE(12, "range"),
    SELECT_RANGE(13, "select_range"),
    VARIABLE(14, "variable"),

    // Extended object types (15-28)
    RULER(15, "ruler"),
    ROOT(16, "root"),
    RICH_TEXT(17, "rich_text"),
    MULTIMEDIA(18, "multimedia"),
    CHART(19, "chart"),
    PICTALK(20, "pictalk"),
    WWW(21, "www"),
    SPLIT(22, "split"),
    ORGANIZER(23, "organizer"),
    TREE(24, "tree"),
    TAB(25, "tab"),
    PROGRESS(26, "progress"),
    TOOLBAR(27, "toolbar"),
    SLIDER(28, "slider");

    private final int code;
    private final String fdoName;

    ObjectType(int code, String fdoName) {
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
     * Look up an ObjectType by its binary code.
     * @param code The binary code (0-28)
     * @return The matching ObjectType, or null if not found
     */
    public static ObjectType fromCode(int code) {
        for (ObjectType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * Look up an ObjectType by its FDO text name.
     * @param name The FDO name (e.g., "ind_group", "trigger")
     * @return The matching ObjectType, or null if not found
     */
    public static ObjectType fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (ObjectType type : values()) {
            if (type.fdoName.equals(lower)) {
                return type;
            }
        }
        // Handle aliases
        return switch (lower) {
            case "group" -> ORG_GROUP;
            case "independent" -> IND_GROUP;
            case "dynamic_multi_list" -> DMS_LIST;
            case "static_multi_list" -> SMS_LIST;
            case "dynamic_list" -> DSS_LIST;
            case "static_list" -> SSS_LIST;
            case "editable_view" -> EDIT_VIEW;
            case "selectable_boolean" -> SELECT_BOOLEAN;
            case "selectable_range" -> SELECT_RANGE;
            default -> null;
        };
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "object_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        ObjectType type = fromCode(code);
        return type != null ? type.fdoName : String.format("object_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (org_group) for unknown names
     */
    public static int codeFromName(String name) {
        ObjectType type = fromName(name);
        return type != null ? type.code : 0;
    }
}
