package com.atomforge.fdo.text;

/**
 * Represents a token from the FDO source lexer.
 */
public record FdoToken(
    TokenType type,
    String value,
    int line,
    int column
) {
    /**
     * Create a simple token with just type and value.
     */
    public static FdoToken of(TokenType type, String value) {
        return new FdoToken(type, value, 0, 0);
    }

    /**
     * Create a token with position info.
     */
    public static FdoToken at(TokenType type, String value, int line, int column) {
        return new FdoToken(type, value, line, column);
    }

    /**
     * Check if this is an EOF token.
     */
    public boolean isEof() {
        return type == TokenType.EOF;
    }

    /**
     * Check if this is an error token.
     */
    public boolean isError() {
        return type == TokenType.ERROR;
    }

    /**
     * Check if this is a newline token.
     */
    public boolean isNewline() {
        return type == TokenType.NEWLINE;
    }

    @Override
    public String toString() {
        if (value == null || value.isEmpty()) {
            return type.name();
        }
        return type.name() + "(" + value + ")";
    }
}
