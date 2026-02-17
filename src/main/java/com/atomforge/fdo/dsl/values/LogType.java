package com.atomforge.fdo.dsl.values;

/**
 * FDO log types for mat_log_object atoms.
 *
 * Specifies how object content should be logged.
 */
public enum LogType {
    SESSION_LOG(0, "session_log"),
    CHAT_LOG(1, "chat_log"),
    IM_LOG(2, "im_log"),
    NO_LOG(3, "no_log");

    private final int code;
    private final String fdoName;

    LogType(int code, String fdoName) {
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
     * Look up a LogType by its binary code.
     * @param code The binary code (0-3)
     * @return The matching LogType, or null if not found
     */
    public static LogType fromCode(int code) {
        for (LogType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * Look up a LogType by its FDO text name.
     * @param name The FDO name (e.g., "session_log", "no_log")
     * @return The matching LogType, or null if not found
     */
    public static LogType fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (LogType type : values()) {
            if (type.fdoName.equals(lower)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get the FDO name for a binary code, with fallback for unknown codes.
     * @param code The binary code
     * @return The FDO name, or "log_type_N" for unknown codes
     */
    public static String nameFromCode(int code) {
        LogType type = fromCode(code);
        return type != null ? type.fdoName : String.format("log_type_%d", code);
    }

    /**
     * Get the binary code for an FDO name, with fallback for unknown names.
     * @param name The FDO name
     * @return The binary code, or 0 (session_log) for unknown names
     */
    public static int codeFromName(String name) {
        LogType type = fromName(name);
        return type != null ? type.code : 0;
    }
}
