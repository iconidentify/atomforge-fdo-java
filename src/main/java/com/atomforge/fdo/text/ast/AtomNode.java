package com.atomforge.fdo.text.ast;

import com.atomforge.fdo.atom.AtomDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Represents a single atom in the AST.
 * An atom has a name and optionally arguments within angle brackets.
 */
public record AtomNode(
    String name,
    List<ArgumentNode> arguments,
    AtomDefinition definition,
    int line,
    int column
) implements FdoNode {

    /**
     * Create an atom node with no arguments.
     */
    public static AtomNode noArgs(String name, AtomDefinition definition, int line, int column) {
        return new AtomNode(name, List.of(), definition, line, column);
    }

    /**
     * Create an atom node with a single argument.
     */
    public static AtomNode singleArg(String name, ArgumentNode arg, AtomDefinition definition, int line, int column) {
        return new AtomNode(name, List.of(arg), definition, line, column);
    }

    /**
     * Check if this atom has arguments.
     */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * Get first argument if present.
     */
    public Optional<ArgumentNode> firstArgument() {
        return arguments.isEmpty() ? Optional.empty() : Optional.of(arguments.get(0));
    }

    /**
     * Check if this atom has a definition from the atom table.
     */
    public boolean hasDefinition() {
        return definition != null;
    }

    /**
     * Get the protocol from the definition, or -1 if unknown.
     */
    public int protocol() {
        return definition != null ? definition.protocol() : -1;
    }

    /**
     * Get the atom number from the definition, or -1 if unknown.
     */
    public int atomNumber() {
        return definition != null ? definition.atomNumber() : -1;
    }
}
