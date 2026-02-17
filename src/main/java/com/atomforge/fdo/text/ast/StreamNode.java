package com.atomforge.fdo.text.ast;

import java.util.List;

/**
 * Represents a stream of atoms in the AST.
 * A stream is a sequence of atom nodes, typically bounded by
 * uni_start_stream and uni_end_stream.
 */
public record StreamNode(
    List<AtomNode> atoms,
    int line,
    int column
) implements FdoNode {

    /**
     * Create an empty stream.
     */
    public static StreamNode empty(int line, int column) {
        return new StreamNode(List.of(), line, column);
    }

    /**
     * Check if this stream is empty.
     */
    public boolean isEmpty() {
        return atoms.isEmpty();
    }

    /**
     * Get the number of atoms in this stream.
     */
    public int size() {
        return atoms.size();
    }

    /**
     * Get atom at index.
     */
    public AtomNode get(int index) {
        return atoms.get(index);
    }
}
