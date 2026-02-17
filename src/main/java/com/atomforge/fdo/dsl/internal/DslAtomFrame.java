package com.atomforge.fdo.dsl.internal;

import com.atomforge.fdo.dsl.atoms.DslAtom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Intermediate representation of an atom and its arguments in the DSL.
 *
 * This is a mutable builder-style object used during DSL construction.
 * It holds the atom reference and its arguments, which can be:
 * - Primitive values (String, Number, Boolean)
 * - DSL value enums (ObjectType, Orientation, etc.)
 * - Nested streams (List of DslAtomFrame)
 * - Raw byte arrays
 */
public final class DslAtomFrame {

    private final DslAtom atom;
    private final List<Object> args;
    private List<DslAtomFrame> nestedStream;

    /**
     * Create a frame for an atom with no arguments.
     */
    public DslAtomFrame(DslAtom atom) {
        this.atom = atom;
        this.args = new ArrayList<>();
        this.nestedStream = null;
    }

    /**
     * Create a frame for an atom with the given arguments.
     */
    public DslAtomFrame(DslAtom atom, Object... args) {
        this.atom = atom;
        this.args = new ArrayList<>(args.length);
        Collections.addAll(this.args, args);
        this.nestedStream = null;
    }

    /**
     * @return The atom this frame represents
     */
    public DslAtom atom() {
        return atom;
    }

    /**
     * @return The arguments for this atom (unmodifiable view)
     */
    public List<Object> args() {
        return Collections.unmodifiableList(args);
    }

    /**
     * @return True if this frame has a nested stream
     */
    public boolean hasNestedStream() {
        return nestedStream != null && !nestedStream.isEmpty();
    }

    /**
     * @return The nested stream, or null if none
     */
    public List<DslAtomFrame> nestedStream() {
        return nestedStream != null
            ? Collections.unmodifiableList(nestedStream)
            : Collections.emptyList();
    }

    /**
     * Add an argument to this frame.
     * @param arg The argument value
     * @return this frame for chaining
     */
    public DslAtomFrame addArg(Object arg) {
        args.add(arg);
        return this;
    }

    /**
     * Set the nested stream for this frame.
     * @param frames The nested frames
     * @return this frame for chaining
     */
    public DslAtomFrame withNestedStream(List<DslAtomFrame> frames) {
        this.nestedStream = new ArrayList<>(frames);
        return this;
    }

    /**
     * Get the first argument as a specific type.
     * @param type The expected type
     * @return The first argument cast to the type, or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getArg(int index, Class<T> type) {
        if (index < 0 || index >= args.size()) {
            return null;
        }
        Object arg = args.get(index);
        if (type.isInstance(arg)) {
            return (T) arg;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(atom.atomName());
        if (!args.isEmpty()) {
            sb.append(" <");
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(args.get(i));
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
