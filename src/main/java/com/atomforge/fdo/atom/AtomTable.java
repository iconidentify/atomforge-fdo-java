package com.atomforge.fdo.atom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lookup table for atom definitions.
 * Provides O(1) lookup by name or by protocol+atom number.
 */
public final class AtomTable {

    private static volatile AtomTable DEFAULT_INSTANCE;

    private final Map<String, AtomDefinition> byName;
    private final Map<Long, AtomDefinition> byKey;
    private final List<AtomDefinition> allAtoms;

    private AtomTable(List<AtomDefinition> atoms) {
        this.allAtoms = List.copyOf(atoms);
        this.byName = new HashMap<>(atoms.size());
        this.byKey = new HashMap<>(atoms.size());

        for (AtomDefinition atom : atoms) {
            byName.put(atom.canonicalName(), atom);
            byKey.put(atom.key(), atom);
        }
    }

    /**
     * Load the default atom table with all registered atoms.
     * Uses lazy initialization with caching for performance.
     */
    public static AtomTable loadDefault() {
        if (DEFAULT_INSTANCE == null) {
            synchronized (AtomTable.class) {
                if (DEFAULT_INSTANCE == null) {
                    DEFAULT_INSTANCE = new AtomTable(AtomRegistry.getAllAtoms());
                }
            }
        }
        return DEFAULT_INSTANCE;
    }

    /**
     * Create an atom table from a custom list of atoms.
     */
    public static AtomTable fromAtoms(List<AtomDefinition> atoms) {
        return new AtomTable(atoms);
    }

    /**
     * Find atom by name (case-insensitive).
     */
    public Optional<AtomDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name.toLowerCase()));
    }

    /**
     * Find atom by protocol and atom number.
     */
    public Optional<AtomDefinition> findByProtocolAtom(int protocol, int atomNumber) {
        long key = ((long) protocol << 16) | atomNumber;
        return Optional.ofNullable(byKey.get(key));
    }

    /**
     * Check if an atom exists by name.
     */
    public boolean containsName(String name) {
        return findByName(name).isPresent();
    }

    /**
     * Check if an atom exists by protocol and atom number.
     */
    public boolean containsProtocolAtom(int protocol, int atomNumber) {
        return findByProtocolAtom(protocol, atomNumber).isPresent();
    }

    /**
     * Get all atoms as a stream.
     */
    public Stream<AtomDefinition> allAtoms() {
        return allAtoms.stream();
    }

    /**
     * Get the total number of atoms.
     */
    public int size() {
        return allAtoms.size();
    }

    /**
     * Get all atoms for a specific protocol.
     */
    public Stream<AtomDefinition> atomsForProtocol(int protocol) {
        return allAtoms.stream()
            .filter(atom -> atom.protocol() == protocol);
    }
}
