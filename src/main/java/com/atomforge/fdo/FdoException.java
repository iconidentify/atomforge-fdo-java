package com.atomforge.fdo;

/**
 * Base exception for FDO compilation and decompilation errors.
 */
public class FdoException extends Exception {

    private final ErrorCode code;
    private final int line;
    private final int column;

    public FdoException(String message) {
        super(message);
        this.code = ErrorCode.UNKNOWN;
        this.line = -1;
        this.column = -1;
    }

    public FdoException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.UNKNOWN;
        this.line = -1;
        this.column = -1;
    }

    public FdoException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.line = -1;
        this.column = -1;
    }

    public FdoException(ErrorCode code, String message, int line, int column) {
        super(message);
        this.code = code;
        this.line = line;
        this.column = column;
    }

    public ErrorCode getCode() {
        return code;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    @Override
    public String getMessage() {
        if (hasLocation()) {
            return String.format("Line %d, column %d: %s", line, column, super.getMessage());
        }
        return super.getMessage();
    }

    /**
     * Error codes for FDO compilation and decompilation errors.
     */
    public enum ErrorCode {
        UNKNOWN,
        MISSING_QUOTE,
        MISSING_OPEN_BRACKET,
        MISSING_CLOSE_BRACKET,
        MISSING_COMMA,
        UNRECOGNIZED_ATOM,
        BAD_ARGUMENT_FORMAT,
        BAD_NUMBER_FORMAT,
        BAD_STRING_FORMAT,
        BAD_GID_FORMAT,
        UNRECOGNIZED_ENUM,
        VALUE_TOO_LARGE,
        INVALID_BINARY_FORMAT,
        UNEXPECTED_EOF,
        INVALID_STYLE,
        MISSING_ANGLE_BRACKET,
        BUFFER_TOO_SMALL
    }
}
