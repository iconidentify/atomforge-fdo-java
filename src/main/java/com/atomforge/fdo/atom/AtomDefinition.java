package com.atomforge.fdo.atom;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Immutable definition of a single atom.
 * Contains protocol, atom number, name, type, and formatting flags.
 */
public record AtomDefinition(
    int protocol,
    int atomNumber,
    String name,
    AtomType type,
    EnumSet<AtomFlags> flags
) {
    public AtomDefinition {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(flags, "flags cannot be null");
        if (protocol < 0 || protocol > 127) {
            throw new IllegalArgumentException("protocol must be 0-127, got: " + protocol);
        }
        if (atomNumber < 0 || atomNumber > 255) {
            throw new IllegalArgumentException("atomNumber must be 0-255, got: " + atomNumber);
        }
        // Make defensive copy of flags
        flags = EnumSet.copyOf(flags);
    }

    /**
     * Convenience constructor for atoms without flags
     */
    public AtomDefinition(int protocol, int atomNumber, String name, AtomType type) {
        this(protocol, atomNumber, name, type, EnumSet.noneOf(AtomFlags.class));
    }

    /**
     * Create atom definition with flags specified as varargs
     */
    public static AtomDefinition of(int protocol, int atomNumber, String name, AtomType type, AtomFlags... flags) {
        EnumSet<AtomFlags> flagSet = flags.length > 0
            ? EnumSet.of(flags[0], flags)
            : EnumSet.noneOf(AtomFlags.class);
        return new AtomDefinition(protocol, atomNumber, name, type, flagSet);
    }

    /**
     * Get canonical name in lowercase with underscores
     */
    public String canonicalName() {
        return name.toLowerCase();
    }

    /**
     * Check if this atom increases indentation
     */
    public boolean isIndent() {
        return flags.contains(AtomFlags.INDENT);
    }

    /**
     * Check if this atom decreases indentation
     */
    public boolean isOutdent() {
        return flags.contains(AtomFlags.OUTDENT);
    }

    /**
     * Check if this atom marks end of stream
     */
    public boolean isEos() {
        return flags.contains(AtomFlags.EOS);
    }

    /**
     * Get unique key for lookup (protocol << 16 | atom)
     */
    public long key() {
        return ((long) protocol << 16) | atomNumber;
    }

    @Override
    public String toString() {
        return String.format("AtomDefinition[proto=%d, atom=%d, name=%s, type=%s, flags=%s]",
            protocol, atomNumber, name, type, flags);
    }
}
