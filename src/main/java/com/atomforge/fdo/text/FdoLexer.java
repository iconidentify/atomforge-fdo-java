package com.atomforge.fdo.text;

import com.atomforge.fdo.FdoException;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer/tokenizer for FDO source text.
 * Converts FDO source into a stream of tokens.
 */
public final class FdoLexer {

    private final String source;
    private int pos;
    private int line;
    private int column;
    private int tokenStart;
    private int tokenLine;
    private int tokenColumn;

    public FdoLexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    /**
     * Tokenize the entire source into a list of tokens.
     * Whitespace and newlines are preserved for formatting.
     */
    public List<FdoToken> tokenize() throws FdoException {
        List<FdoToken> tokens = new ArrayList<>();
        FdoToken token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.type() != TokenType.EOF);
        return tokens;
    }

    /**
     * Tokenize, skipping whitespace tokens.
     */
    public List<FdoToken> tokenizeSkipWhitespace() throws FdoException {
        List<FdoToken> tokens = new ArrayList<>();
        FdoToken token;
        do {
            token = nextToken();
            if (token.type() != TokenType.WHITESPACE) {
                tokens.add(token);
            }
        } while (token.type() != TokenType.EOF);
        return tokens;
    }

    /**
     * Get the next token from the source.
     */
    public FdoToken nextToken() throws FdoException {
        skipWhitespaceExceptNewline();

        if (isAtEnd()) {
            return makeToken(TokenType.EOF, "");
        }

        markTokenStart();
        char c = advance();

        // Single character tokens
        switch (c) {
            case '<':
                return makeToken(TokenType.ANGLE_OPEN, "<");
            case '>':
                return makeToken(TokenType.ANGLE_CLOSE, ">");
            case ',':
                return makeToken(TokenType.COMMA, ",");
            case '|':
                return makeToken(TokenType.PIPE, "|");
            case '\n':
                return makeToken(TokenType.NEWLINE, "\n");
            case '\r':
                if (peek() == '\n') {
                    advance();
                }
                return makeToken(TokenType.NEWLINE, "\n");
            case '"':
                return scanString();
            case ';':
                // Comment - skip to end of line
                skipToEndOfLine();
                return nextToken(); // Get next actual token
        }

        // Identifiers, numbers, GIDs
        if (isAlpha(c) || c == '_') {
            return scanIdentifier();
        }
        if (isDigit(c) || c == '-') {
            return scanNumberOrGid();
        }

        // Unknown character - error
        return makeToken(TokenType.ERROR, String.valueOf(c));
    }

    /**
     * Scan a double-quoted string literal.
     */
    private FdoToken scanString() throws FdoException {
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd() && peek() != '"') {
            char c = peek();
            if (c == '\n' || c == '\r') {
                throw new FdoException(FdoException.ErrorCode.MISSING_QUOTE,
                    "Unterminated string at line " + tokenLine);
            }
            if (c == '\\' && pos + 1 < source.length()) {
                advance(); // consume backslash
                char escaped = advance();
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> {
                        // Keep double backslash as double backslash for later unescaping
                        // This preserves \\x00 -> \x00 (literal) vs \x00 -> NUL byte
                        sb.append('\\');
                        sb.append('\\');
                    }
                    default -> {
                        sb.append('\\');
                        sb.append(escaped);
                    }
                }
            } else {
                sb.append(advance());
            }
        }

        if (isAtEnd()) {
            throw new FdoException(FdoException.ErrorCode.MISSING_QUOTE,
                "Unterminated string at line " + tokenLine);
        }

        advance(); // consume closing quote
        return makeToken(TokenType.STRING, sb.toString());
    }

    /**
     * Scan an identifier (atom name, keyword, orientation code, etc.)
     */
    private FdoToken scanIdentifier() {
        while (!isAtEnd() && (isAlphaNumeric(peek()) || peek() == '_')) {
            advance();
        }

        String value = source.substring(tokenStart, pos);

        // Check for hex value (e.g., "00x")
        if (value.endsWith("x") && value.length() > 1) {
            String prefix = value.substring(0, value.length() - 1);
            if (prefix.chars().allMatch(c -> isHexDigit((char) c))) {
                return makeToken(TokenType.HEX_VALUE, value);
            }
        }

        // Check if it's an atom name (contains underscore)
        if (value.contains("_")) {
            return makeToken(TokenType.ATOM_NAME, value);
        }

        return makeToken(TokenType.IDENTIFIER, value);
    }

    /**
     * Scan a number, GID (like "32-105"), or hex value (like "00x", "0ax", "4bx").
     * FDO hex notation is: hex digits followed by 'x' (e.g., 0ax = 0x0a = 10 decimal)
     */
    private FdoToken scanNumberOrGid() {
        // Handle negative sign
        boolean hasNegative = source.charAt(tokenStart) == '-';
        if (hasNegative && (isAtEnd() || !isDigit(peek()))) {
            // Just a dash - might be part of something else
            return makeToken(TokenType.ERROR, "-");
        }

        // First, try to scan as a potential hex value (digits + hex letters followed by 'x')
        // Save position for potential backtrack
        int savedPos = pos;
        int savedLine = line;
        int savedColumn = column;

        // Scan all hex digits (0-9, a-f, A-F)
        while (!isAtEnd() && isHexDigit(peek())) {
            advance();
        }

        // Check for hex value ending in 'x' (e.g., "00x", "0ax", "4bx")
        if (!isAtEnd() && (peek() == 'x' || peek() == 'X')) {
            advance(); // consume the 'x'
            String value = source.substring(tokenStart, pos);
            return makeToken(TokenType.HEX_VALUE, value);
        }

        // Not a hex value - backtrack and re-scan as number/GID
        pos = savedPos;
        line = savedLine;
        column = savedColumn;

        // Scan only decimal digits for number/GID
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }

        // Check for GID (contains dashes between digits)
        if (!isAtEnd() && peek() == '-' && pos + 1 < source.length() && isDigit(source.charAt(pos + 1))) {
            // This is a GID like "32-105"
            while (!isAtEnd() && (isDigit(peek()) || peek() == '-')) {
                advance();
            }
            String value = source.substring(tokenStart, pos);
            return makeToken(TokenType.GID, value);
        }

        String value = source.substring(tokenStart, pos);
        return makeToken(TokenType.NUMBER, value);
    }

    /**
     * Skip whitespace except newlines (newlines are significant for formatting).
     */
    private void skipWhitespaceExceptNewline() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t') {
                advance();
            } else {
                break;
            }
        }
    }

    /**
     * Skip to end of line (for comments).
     */
    private void skipToEndOfLine() {
        while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
            advance();
        }
    }

    private void markTokenStart() {
        tokenStart = pos;
        tokenLine = line;
        tokenColumn = column;
    }

    private FdoToken makeToken(TokenType type, String value) {
        return FdoToken.at(type, value, tokenLine, tokenColumn);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= source.length()) return '\0';
        return source.charAt(pos + 1);
    }

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Get current line number.
     */
    public int getLine() {
        return line;
    }

    /**
     * Get current column number.
     */
    public int getColumn() {
        return column;
    }
}
