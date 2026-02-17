package com.atomforge.fdo.model;

import com.atomforge.fdo.atom.AtomType;

import java.util.Optional;

/**
 * Represents a single decoded FDO atom with type-safe value access.
 *
 * <p>Usage:
 * <pre>
 * FdoAtom atom = stream.findFirst("de_data").orElseThrow();
 * String value = atom.getString();
 * </pre>
 *
 * <p>The rawData field preserves the original binary encoding for round-trip fidelity.
 * When re-encoding, if rawData is present, it will be used directly rather than
 * re-encoding from the value, ensuring byte-for-byte identical output.
 */
public record FdoAtom(
        String name,
        int protocol,
        int atomNumber,
        AtomType type,
        FdoValue value,
        byte[] rawData  // Preserves original binary for round-trip fidelity
) {

    public FdoAtom {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (protocol < 0 || protocol > 127) {
            throw new IllegalArgumentException("protocol must be 0-127, got: " + protocol);
        }
        if (atomNumber < 0 || atomNumber > 255) {
            throw new IllegalArgumentException("atomNumber must be 0-255, got: " + atomNumber);
        }
        if (value == null) {
            value = FdoValue.EmptyValue.instance();
        }
        // rawData can be null (for programmatically constructed atoms)
        // or a copy of the original binary (for decoded atoms)
        if (rawData != null) {
            rawData = rawData.clone();  // Defensive copy
        }
    }

    /**
     * Convenience constructor without rawData (for programmatically constructed atoms).
     */
    public FdoAtom(String name, int protocol, int atomNumber, AtomType type, FdoValue value) {
        this(name, protocol, atomNumber, type, value, null);
    }

    // ========== Type-safe accessors ==========

    /**
     * Get value as string. Throws if not a string value.
     */
    public String getString() {
        if (value instanceof FdoValue.StringValue sv) {
            return sv.value();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a string value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get value as string if present.
     */
    public Optional<String> getStringOpt() {
        if (value instanceof FdoValue.StringValue sv) {
            return Optional.of(sv.value());
        }
        return Optional.empty();
    }

    /**
     * Get value as number. Throws if not a numeric value.
     */
    public long getNumber() {
        if (value instanceof FdoValue.NumberValue nv) {
            return nv.value();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a number value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get value as number if present.
     */
    public Optional<Long> getNumberOpt() {
        if (value instanceof FdoValue.NumberValue nv) {
            return Optional.of(nv.value());
        }
        return Optional.empty();
    }

    /**
     * Get value as GID. Throws if not a GID value.
     */
    public FdoGid getGid() {
        if (value instanceof FdoValue.GidValue gv) {
            return gv.gid();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a GID value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get value as GID if present.
     */
    public Optional<FdoGid> getGidOpt() {
        if (value instanceof FdoValue.GidValue gv) {
            return Optional.of(gv.gid());
        }
        return Optional.empty();
    }

    /**
     * Get value as boolean. Throws if not a boolean value.
     */
    public boolean getBoolean() {
        if (value instanceof FdoValue.BooleanValue bv) {
            return bv.value();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a boolean value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get value as boolean if present.
     */
    public Optional<Boolean> getBooleanOpt() {
        if (value instanceof FdoValue.BooleanValue bv) {
            return Optional.of(bv.value());
        }
        return Optional.empty();
    }

    /**
     * Get value as nested stream. Throws if not a stream value.
     */
    public FdoStream getNestedStream() {
        if (value instanceof FdoValue.StreamValue sv) {
            return sv.stream();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a stream value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get value as nested stream if present.
     */
    public Optional<FdoStream> getNestedStreamOpt() {
        if (value instanceof FdoValue.StreamValue sv) {
            return Optional.of(sv.stream());
        }
        return Optional.empty();
    }

    /**
     * Get raw bytes. Works for RawValue, returns encoded bytes for other types.
     */
    public byte[] getRawBytes() {
        if (value instanceof FdoValue.RawValue rv) {
            return rv.data();
        }
        throw new IllegalStateException("Atom '" + name + "' is not a raw value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get orientation code string. Throws if not an orientation value.
     */
    public String getOrientation() {
        if (value instanceof FdoValue.OrientationValue ov) {
            return ov.code();
        }
        throw new IllegalStateException("Atom '" + name + "' is not an orientation value, got: " + value.getClass().getSimpleName());
    }

    /**
     * Get object type value. Throws if not an object type value.
     */
    public FdoValue.ObjectTypeValue getObjectType() {
        if (value instanceof FdoValue.ObjectTypeValue otv) {
            return otv;
        }
        throw new IllegalStateException("Atom '" + name + "' is not an object type value, got: " + value.getClass().getSimpleName());
    }

    // ========== Type checking ==========

    /**
     * Check if value is a string.
     */
    public boolean isString() {
        return value.isString();
    }

    /**
     * Check if value is a number.
     */
    public boolean isNumber() {
        return value.isNumber();
    }

    /**
     * Check if value is a GID.
     */
    public boolean isGid() {
        return value.isGid();
    }

    /**
     * Check if value is a boolean.
     */
    public boolean isBoolean() {
        return value.isBoolean();
    }

    /**
     * Check if atom has a nested stream.
     */
    public boolean hasNestedStream() {
        return value.isStream();
    }

    /**
     * Check if value is empty (no data).
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * Check if value is a list.
     */
    public boolean isList() {
        return value.isList();
    }

    /**
     * Get unique key for this atom (protocol << 16 | atom).
     */
    public long key() {
        return ((long) protocol << 16) | atomNumber;
    }

    /**
     * Check if this atom has rawData preserved for round-trip encoding.
     */
    public boolean hasRawData() {
        return rawData != null;
    }
}
