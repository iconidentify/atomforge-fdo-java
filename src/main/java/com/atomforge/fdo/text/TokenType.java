package com.atomforge.fdo.text;

/**
 * Token types for FDO source lexer.
 */
public enum TokenType {
    /** Atom identifier (e.g., "uni_start_stream", "mat_object_id") */
    ATOM_NAME,

    /** Opening angle bracket < */
    ANGLE_OPEN,

    /** Closing angle bracket > */
    ANGLE_CLOSE,

    /** Comma separator */
    COMMA,

    /** Pipe/vertical bar for OR */
    PIPE,

    /** Double-quoted string literal */
    STRING,

    /** Hex value like "00x" */
    HEX_VALUE,

    /** Numeric value (integer) */
    NUMBER,

    /** GID like "32-105" or "1-0-1329" */
    GID,

    /** Identifier like "vcf", "center", "yes", "no" */
    IDENTIFIER,

    /** End of line (significant for formatting) */
    NEWLINE,

    /** End of file */
    EOF,

    /** Whitespace (usually skipped, but tracked for formatting) */
    WHITESPACE,

    /** Invalid/error token */
    ERROR
}
