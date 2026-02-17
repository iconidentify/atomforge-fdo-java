package com.atomforge.fdo.text;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.text.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parser for FDO source text.
 * Converts tokens into an AST.
 */
public final class FdoParser {

    private final List<FdoToken> tokens;
    private final AtomTable atomTable;
    private int pos;

    public FdoParser(List<FdoToken> tokens, AtomTable atomTable) {
        this.tokens = tokens;
        this.atomTable = atomTable;
        this.pos = 0;
    }

    /**
     * Parse the tokens into a stream of atoms.
     */
    public StreamNode parse() throws FdoException {
        List<AtomNode> atoms = new ArrayList<>();
        int startLine = current().line();
        int startColumn = current().column();

        while (!isAtEnd()) {
            skipNewlines();
            if (isAtEnd()) break;

            AtomNode atom = parseAtom();
            if (atom != null) {
                atoms.add(atom);
            }
        }

        return new StreamNode(atoms, startLine, startColumn);
    }

    /**
     * Parse a single atom.
     */
    private AtomNode parseAtom() throws FdoException {
        if (!check(TokenType.ATOM_NAME) && !check(TokenType.IDENTIFIER)) {
            // Skip unexpected token to prevent infinite loop
            if (!isAtEnd()) {
                advance();
            }
            return null;
        }

        FdoToken nameToken = advance();
        String atomName = nameToken.value();
        int line = nameToken.line();
        int column = nameToken.column();

        // Look up atom definition
        Optional<AtomDefinition> defOpt = atomTable.findByName(atomName);
        AtomDefinition definition = defOpt.orElse(null);

        // Check for arguments in angle brackets (may be on next line)
        skipNewlines();
        List<ArgumentNode> arguments = List.of();
        if (check(TokenType.ANGLE_OPEN)) {
            arguments = parseArguments();
        }

        return new AtomNode(atomName, arguments, definition, line, column);
    }

    /**
     * Parse arguments within angle brackets.
     * Handles both simple arguments <value> and multi-line stream arguments:
     *   <
     *   uni_start_stream
     *   uni_end_stream
     *   >
     */
    private List<ArgumentNode> parseArguments() throws FdoException {
        FdoToken openBracket = expect(TokenType.ANGLE_OPEN, "Expected '<'");

        skipNewlines();

        // Empty arguments <>
        if (check(TokenType.ANGLE_CLOSE)) {
            advance();
            return List.of();
        }

        // Check if this is a multi-line stream argument:
        // After <, if we see an ATOM_NAME that's a known atom (contains underscore),
        // treat the entire contents as a nested stream
        if (isMultiLineStreamArgument()) {
            return parseMultiLineStreamArgument(openBracket);
        }

        List<ArgumentNode> args = new ArrayList<>();
        args.add(parseArgument());

        // Handle comma-separated arguments
        while (check(TokenType.COMMA)) {
            advance(); // consume comma
            skipNewlines();
            args.add(parseArgument());
        }

        skipNewlines();
        expect(TokenType.ANGLE_CLOSE, "Expected '>'");

        // If we have multiple simple args, wrap in ListArg
        if (args.size() > 1) {
            // Check if this is an object type + title pattern
            if (args.size() == 2 &&
                args.get(0) instanceof ArgumentNode.IdentifierArg idArg &&
                args.get(1) instanceof ArgumentNode.StringArg strArg) {
                // Object type pattern: <ind_group, "Title">
                return List.of(new ArgumentNode.ObjectTypeArg(
                    idArg.value(), strArg.value(), idArg.line(), idArg.column()));
            }
            // Check for atom name + title pattern
            if (args.size() == 2 &&
                args.get(0) instanceof ArgumentNode.IdentifierArg idArg &&
                args.get(1) instanceof ArgumentNode.StringArg strArg) {
                return List.of(new ArgumentNode.ObjectTypeArg(
                    idArg.value(), strArg.value(), idArg.line(), idArg.column()));
            }
            // Regular list
            int line = args.get(0).line();
            int column = args.get(0).column();
            return List.of(new ArgumentNode.ListArg(args, line, column));
        }

        return args;
    }

    /**
     * Check if current position is the start of a multi-line stream argument.
     * This is when we see an ATOM_NAME after < that is a known FDO atom.
     */
    private boolean isMultiLineStreamArgument() {
        if (!check(TokenType.ATOM_NAME) && !check(TokenType.IDENTIFIER)) {
            return false;
        }

        String name = current().value();

        // Known FDO atoms contain underscores (uni_start_stream, de_ez_send_form, etc.)
        // Simple identifiers like 'vcf', 'yes', 'center' don't have underscores
        if (!name.contains("_")) {
            return false;
        }

        // Check if it's a recognized atom
        return atomTable.containsName(name);
    }

    /**
     * Parse a multi-line stream argument - atoms between < and >.
     * Also handles trailing data after atom references (e.g., <uni_command, 00x, 00x, ...>).
     */
    private List<ArgumentNode> parseMultiLineStreamArgument(FdoToken openBracket) throws FdoException {
        int line = openBracket.line();
        int column = openBracket.column();

        List<AtomNode> atoms = new ArrayList<>();
        List<ArgumentNode> trailingData = new ArrayList<>();

        while (!check(TokenType.ANGLE_CLOSE) && !isAtEnd()) {
            skipNewlines();
            if (check(TokenType.ANGLE_CLOSE)) break;

            // Try to parse an atom
            AtomNode atom = parseAtom();
            if (atom != null) {
                atoms.add(atom);
                
                // Check if there's trailing data after this atom (comma-separated hex/number values)
                // Only collect trailing data if:
                // 1. We have exactly one atom so far
                // 2. That atom has a definition (is a known atom)
                // 3. The next token is a comma followed by hex/number values
                skipNewlines();
                if (atoms.size() == 1 && atom.hasDefinition() && check(TokenType.COMMA)) {
                    // Collect trailing data: comma-separated hex/number values
                    while (check(TokenType.COMMA)) {
                        advance(); // consume comma
                        skipNewlines();
                        
                        // Parse trailing value (hex or number)
                        if (check(TokenType.HEX_VALUE)) {
                            FdoToken hexToken = advance();
                            trailingData.add(new ArgumentNode.HexArg(hexToken.value(), hexToken.line(), hexToken.column()));
                        } else if (check(TokenType.NUMBER)) {
                            FdoToken numToken = advance();
                            trailingData.add(new ArgumentNode.NumberArg(
                                Long.parseLong(numToken.value()), numToken.line(), numToken.column()));
                        } else {
                            // Not a trailing data value, break out
                            break;
                        }
                        skipNewlines();
                    }
                    // If we collected trailing data, we're done (don't parse more atoms)
                    if (!trailingData.isEmpty()) {
                        break;
                    }
                }
            } else {
                // Couldn't parse as atom, might be error - skip to prevent infinite loop
                if (!isAtEnd()) {
                    advance();
                }
            }
            skipNewlines();
        }

        expect(TokenType.ANGLE_CLOSE, "Expected '>' to close stream argument");

        StreamNode stream = new StreamNode(atoms, line, column);
        ArgumentNode.NestedStreamArg nestedArg = new ArgumentNode.NestedStreamArg(stream, trailingData, line, column);
        return List.of(nestedArg);
    }

    /**
     * Parse a single argument value.
     */
    private ArgumentNode parseArgument() throws FdoException {
        skipNewlines();

        FdoToken token = current();

        switch (token.type()) {
            case STRING:
                advance();
                return new ArgumentNode.StringArg(token.value(), token.line(), token.column());

            case NUMBER:
                advance();
                return new ArgumentNode.NumberArg(
                    Long.parseLong(token.value()), token.line(), token.column());

            case HEX_VALUE:
                advance();
                return new ArgumentNode.HexArg(token.value(), token.line(), token.column());

            case GID:
                advance();
                return new ArgumentNode.GidArg(token.value(), token.line(), token.column());

            case IDENTIFIER:
            case ATOM_NAME:
                // Check if this is a nested stream (atom followed by newline and more atoms)
                // vs a simple identifier (like 'vcf', 'yes', 'ind_group')
                if (looksLikeNestedStream()) {
                    return parseNestedStreamContent();
                }
                return parseIdentifierOrPiped();

            case ANGLE_OPEN:
                return parseNestedStream();

            default:
                throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                    "Unexpected token in argument at line " + token.line() + ": " + token);
        }
    }

    /**
     * Check if current position looks like the start of a nested stream content.
     * A nested stream has atoms followed by newlines and more atoms until '>'.
     */
    private boolean looksLikeNestedStream() {
        // Save position for lookahead
        int savedPos = pos;

        // Skip current atom name
        advance();
        skipNewlines();

        // If next is ANGLE_CLOSE, it's a single atom in nested stream
        // If next is another ATOM_NAME/IDENTIFIER (not followed by < on same line), it's nested stream content
        boolean isNested = false;
        if (check(TokenType.ANGLE_CLOSE)) {
            // Single atom in nested stream - but we need to check if it looks like a stream atom
            // Actually, restore and check if it's a known atom name
            pos = savedPos;
            String name = current().value();
            // Known FDO atoms have underscores - simple identifiers (vcf, yes, center) don't
            isNested = name.contains("_") && atomTable.containsName(name);
        } else if (check(TokenType.ATOM_NAME) || check(TokenType.IDENTIFIER)) {
            // Multiple atoms = definitely a nested stream
            isNested = true;
        }

        // Restore position
        pos = savedPos;
        return isNested;
    }

    /**
     * Parse nested stream content (atoms without outer angle brackets).
     */
    private ArgumentNode parseNestedStreamContent() throws FdoException {
        FdoToken first = current();
        int line = first.line();
        int column = first.column();

        List<AtomNode> atoms = new ArrayList<>();

        while (!check(TokenType.ANGLE_CLOSE) && !isAtEnd()) {
            skipNewlines();
            if (check(TokenType.ANGLE_CLOSE)) break;

            AtomNode atom = parseAtom();
            if (atom != null) {
                atoms.add(atom);
            }
            skipNewlines();
        }

        StreamNode stream = new StreamNode(atoms, line, column);
        return new ArgumentNode.NestedStreamArg(stream, line, column);
    }

    /**
     * Parse identifier that may have pipe (OR) continuation.
     * Accepts IDENTIFIER, ATOM_NAME, or NUMBER after pipe.
     */
    private ArgumentNode parseIdentifierOrPiped() throws FdoException {
        FdoToken first = advance();
        ArgumentNode firstArg = new ArgumentNode.IdentifierArg(
            first.value(), first.line(), first.column());

        if (!check(TokenType.PIPE)) {
            return firstArg;
        }

        // Piped arguments: left | center | stream_id_header | 4
        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(firstArg);

        while (check(TokenType.PIPE)) {
            advance(); // consume pipe
            skipNewlines();
            FdoToken token = current();

            // Accept IDENTIFIER, ATOM_NAME, or NUMBER after pipe
            if (check(TokenType.IDENTIFIER) || check(TokenType.ATOM_NAME)) {
                FdoToken next = advance();
                parts.add(new ArgumentNode.IdentifierArg(next.value(), next.line(), next.column()));
            } else if (check(TokenType.NUMBER)) {
                FdoToken next = advance();
                parts.add(new ArgumentNode.NumberArg(Long.parseLong(next.value()), next.line(), next.column()));
            } else {
                throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                    "Expected identifier, atom name, or number after '|' at line " + token.line() + ", got " + token);
            }
        }

        return new ArgumentNode.PipedArg(parts, first.line(), first.column());
    }

    /**
     * Parse a nested stream within angle brackets.
     */
    private ArgumentNode parseNestedStream() throws FdoException {
        FdoToken open = expect(TokenType.ANGLE_OPEN, "Expected '<'");
        int line = open.line();
        int column = open.column();

        List<AtomNode> atoms = new ArrayList<>();

        skipNewlines();
        while (!check(TokenType.ANGLE_CLOSE) && !isAtEnd()) {
            skipNewlines();
            if (check(TokenType.ANGLE_CLOSE)) break;

            AtomNode atom = parseAtom();
            if (atom != null) {
                atoms.add(atom);
            }
            skipNewlines();
        }

        expect(TokenType.ANGLE_CLOSE, "Expected '>' to close nested stream");

        StreamNode stream = new StreamNode(atoms, line, column);
        return new ArgumentNode.NestedStreamArg(stream, line, column);
    }

    // === Helper methods ===

    private void skipNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return current().type() == type;
    }

    private FdoToken current() {
        return tokens.get(pos);
    }

    private FdoToken advance() {
        if (!isAtEnd()) pos++;
        return tokens.get(pos - 1);
    }

    private FdoToken expect(TokenType type, String message) throws FdoException {
        if (check(type)) {
            return advance();
        }
        FdoToken token = current();
        throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
            message + " at line " + token.line() + ", got " + token);
    }

    private boolean isAtEnd() {
        return current().type() == TokenType.EOF;
    }

    /**
     * Convenience method to parse FDO source text directly.
     */
    public static StreamNode parse(String source) throws FdoException {
        return parse(source, AtomTable.loadDefault());
    }

    /**
     * Parse FDO source text with a specific atom table.
     */
    public static StreamNode parse(String source, AtomTable atomTable) throws FdoException {
        FdoLexer lexer = new FdoLexer(source);
        List<FdoToken> tokens = lexer.tokenizeSkipWhitespace();
        FdoParser parser = new FdoParser(tokens, atomTable);
        return parser.parse();
    }
}
