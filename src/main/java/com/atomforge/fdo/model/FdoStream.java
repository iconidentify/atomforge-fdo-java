package com.atomforge.fdo.model;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.BinaryDecoder;
import com.atomforge.fdo.binary.BinaryEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Root container for decoded FDO atoms with query and traversal methods.
 *
 * <p>Usage:
 * <pre>
 * FdoStream stream = FdoStream.decode(binaryData);
 *
 * // Find atoms by name
 * String username = stream.findFirst("de_data").orElseThrow().getString();
 * List&lt;FdoAtom&gt; allDeData = stream.findAll("de_data");
 *
 * // Iterate all atoms
 * for (FdoAtom atom : stream.atoms()) {
 *     System.out.println(atom.name());
 * }
 *
 * // Round-trip back to binary
 * byte[] recompiled = stream.toBytes();
 * </pre>
 */
public record FdoStream(List<FdoAtom> atoms) {

    private static final AtomTable ATOM_TABLE = AtomTable.loadDefault();

    public FdoStream {
        if (atoms == null) {
            atoms = List.of();
        } else {
            atoms = List.copyOf(atoms); // immutable
        }
    }

    // ========== Factory Methods ==========

    /**
     * Decode binary FDO data to an FdoStream.
     *
     * @param binary The binary FDO data (pure atom stream, no frame header)
     * @return Decoded FdoStream
     * @throws FdoException if decoding fails
     */
    public static FdoStream decode(byte[] binary) throws FdoException {
        if (binary == null || binary.length == 0) {
            return new FdoStream(List.of());
        }

        BinaryDecoder decoder = new BinaryDecoder(binary);
        List<AtomFrame> frames = decoder.decode();

        List<FdoAtom> atoms = new ArrayList<>(frames.size());
        for (AtomFrame frame : frames) {
            atoms.add(ValueDecoder.decode(frame, ATOM_TABLE));
        }

        return new FdoStream(atoms);
    }

    /**
     * Encode this stream back to binary.
     *
     * @return Binary FDO data
     * @throws FdoException if encoding fails
     */
    public byte[] toBytes() throws FdoException {
        List<AtomFrame> frames = new ArrayList<>(atoms.size());
        for (FdoAtom atom : atoms) {
            frames.add(ValueEncoder.encode(atom));
        }
        return new BinaryEncoder().encode(frames);
    }

    // ========== Query Methods ==========

    /**
     * Find the first atom with the given name.
     *
     * @param atomName The atom name to search for
     * @return Optional containing the first matching atom
     */
    public Optional<FdoAtom> findFirst(String atomName) {
        for (FdoAtom atom : atoms) {
            if (atom.name().equals(atomName)) {
                return Optional.of(atom);
            }
        }
        return Optional.empty();
    }

    /**
     * Find all atoms with the given name.
     *
     * @param atomName The atom name to search for
     * @return List of matching atoms (empty if none found)
     */
    public List<FdoAtom> findAll(String atomName) {
        List<FdoAtom> result = new ArrayList<>();
        for (FdoAtom atom : atoms) {
            if (atom.name().equals(atomName)) {
                result.add(atom);
            }
        }
        return result;
    }

    /**
     * Find all atoms with the given protocol.
     *
     * @param protocol The protocol number (0-127)
     * @return List of matching atoms
     */
    public List<FdoAtom> findByProtocol(int protocol) {
        List<FdoAtom> result = new ArrayList<>();
        for (FdoAtom atom : atoms) {
            if (atom.protocol() == protocol) {
                result.add(atom);
            }
        }
        return result;
    }

    /**
     * Find all atoms matching a predicate.
     *
     * @param predicate The filter predicate
     * @return List of matching atoms
     */
    public List<FdoAtom> filter(Predicate<FdoAtom> predicate) {
        List<FdoAtom> result = new ArrayList<>();
        for (FdoAtom atom : atoms) {
            if (predicate.test(atom)) {
                result.add(atom);
            }
        }
        return result;
    }

    /**
     * Get a stream of atoms for functional operations.
     *
     * @return Stream of atoms
     */
    public Stream<FdoAtom> stream() {
        return atoms.stream();
    }

    // ========== Access Methods ==========

    /**
     * Get atom at index.
     *
     * @param index The index
     * @return Atom at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public FdoAtom get(int index) {
        return atoms.get(index);
    }

    /**
     * Get the number of atoms.
     */
    public int size() {
        return atoms.size();
    }

    /**
     * Check if stream is empty.
     */
    public boolean isEmpty() {
        return atoms.isEmpty();
    }

    /**
     * Check if stream contains an atom with the given name.
     */
    public boolean contains(String atomName) {
        return findFirst(atomName).isPresent();
    }

    // ========== Formatting Methods ==========

    /**
     * Format this stream to FDO source text with default 2-space indentation.
     *
     * @return Formatted FDO source text
     */
    public String toText() {
        return StreamPrettyPrinter.format(this);
    }

    /**
     * Format this stream to FDO source text with custom indentation.
     *
     * @param indent The indentation string (e.g., "  " or "\t")
     * @return Formatted FDO source text
     */
    public String toText(String indent) {
        return StreamPrettyPrinter.format(this, indent);
    }
}
