package com.atomforge.fdo.atom;

/**
 * Atom data type codes.
 * These determine how atom arguments are parsed and encoded.
 */
public enum AtomType {
    /**
     * Raw binary data - no interpretation
     */
    RAW(0x01, "$raw"),

    /**
     * 32-bit integer (big-endian in binary)
     */
    DWORD(0x02, "$dword"),

    /**
     * Null-terminated string
     */
    STRING(0x03, "$string"),

    /**
     * Boolean value (yes/no) - ADA.BIN code 0x04
     */
    BOOL(0x04, "$bool"),

    /**
     * Global ID - format like "32-105" or "1-0-1329"
     */
    GID(0x05, "$gid"),

    /**
     * Nested atom stream - ADA.BIN code 0x06
     */
    STREAM(0x06, "$stream"),

    /**
     * Object type with title - format like "ind_group, \"Title\""
     */
    OBJSTART(0x07, "$objstart"),

    /**
     * Orientation codes (vcf, hef, etc.) - ADA.BIN code 0x08
     */
    ORIENT(0x08, "$orient"),

    /**
     * Magic token argument
     */
    TOKEN(0x09, "$token"),

    /**
     * Token with argument
     */
    TOKENARG(0x0A, "$tokenarg"),

    /**
     * Alert type - ADA.BIN code 0x0B
     */
    ALERT(0x0B, "$alert"),

    /**
     * Atom reference
     */
    ATOM(0x0C, "$atom"),

    /**
     * Color/ruler data - ADA.BIN code 0x0D
     */
    COLORDATA(0x0D, "$color"),

    /**
     * Ignored/internal use
     */
    IGNORE(0x0E, "$ignore"),

    /**
     * Variable lookup - ADA.BIN code 0x0F
     */
    VARLOOKUP(0x0F, "$varlookup"),

    /**
     * Action criterion value - ADA.BIN code 0x10 (was documented as BOOL)
     */
    CRITERION(0x10, "$criterion"),

    /**
     * Unused/legacy BOOL code
     */
    BOOL_LEGACY(0x11, "$bool_legacy"),

    /**
     * Unused/legacy CRITERION code
     */
    CRITERION_LEGACY(0x12, "$criterion_legacy"),

    /**
     * Unused/legacy STREAM code
     */
    STREAM_LEGACY(0x13, "$stream_legacy"),

    /**
     * Variable reference
     */
    VAR(0x14, "$var"),

    /**
     * Variable with dword value
     */
    VARDWORD(0x15, "$vardword"),

    /**
     * Variable with string value
     */
    VARSTRING(0x16, "$varstring"),

    /**
     * Byte list for if-atoms
     */
    BYTELIST(0x17, "$bytelist"),

    /**
     * Alert type - legacy code
     */
    ALERT_LEGACY(0x18, "$alert_legacy");

    private final int code;
    private final String directive;

    AtomType(int code, String directive) {
        this.code = code;
        this.directive = directive;
    }

    public int getCode() {
        return code;
    }

    public String getDirective() {
        return directive;
    }

    /**
     * Find AtomType by directive string.
     */
    public static AtomType fromDirective(String directive) {
        String normalized = directive.toLowerCase().trim();
        for (AtomType type : values()) {
            if (type.directive.equals(normalized)) {
                return type;
            }
        }
        // Handle aliases
        return switch (normalized) {
            case "$object" -> OBJSTART;
            case "$tokenarg" -> TOKENARG;
            default -> RAW; // Default to raw for unknown types
        };
    }
}
