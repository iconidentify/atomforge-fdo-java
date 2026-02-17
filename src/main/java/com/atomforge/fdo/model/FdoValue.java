package com.atomforge.fdo.model;

import java.util.List;

/**
 * Sealed interface representing a decoded FDO value.
 * Provides type-safe access to atom argument data with native Java types.
 *
 * <p>Use pattern matching to handle different value types:
 * <pre>
 * switch (value) {
 *     case FdoValue.StringValue(var s) -> process(s);
 *     case FdoValue.NumberValue(var n) -> process(n);
 *     case FdoValue.GidValue(var gid) -> process(gid);
 *     // ...
 * }
 * </pre>
 */
public sealed interface FdoValue permits
        FdoValue.StringValue,
        FdoValue.NumberValue,
        FdoValue.GidValue,
        FdoValue.BooleanValue,
        FdoValue.OrientationValue,
        FdoValue.ObjectTypeValue,
        FdoValue.StreamValue,
        FdoValue.RawValue,
        FdoValue.ListValue,
        FdoValue.EmptyValue {

    /**
     * String value from STRING type atoms.
     */
    record StringValue(String value) implements FdoValue {
        public StringValue {
            if (value == null) {
                throw new IllegalArgumentException("value cannot be null");
            }
        }
    }

    /**
     * Numeric value from DWORD type atoms.
     * Uses long to handle unsigned 32-bit values.
     */
    record NumberValue(long value) implements FdoValue {}

    /**
     * Global ID value from GID type atoms.
     * Supports both 2-part (type-id) and 3-part (type-subtype-id) formats.
     */
    record GidValue(FdoGid gid) implements FdoValue {
        public GidValue {
            if (gid == null) {
                throw new IllegalArgumentException("gid cannot be null");
            }
        }
    }

    /**
     * Boolean value from BOOL type atoms.
     */
    record BooleanValue(boolean value) implements FdoValue {}

    /**
     * Orientation value from ORIENT type atoms.
     * Encodes vertical/horizontal and justify settings.
     */
    record OrientationValue(String code) implements FdoValue {
        public OrientationValue {
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("code cannot be null or empty");
            }
        }
    }

    /**
     * Object type value from OBJSTART type atoms.
     * Contains object type name and optional title.
     */
    record ObjectTypeValue(String typeName, String title) implements FdoValue {
        public ObjectTypeValue {
            if (typeName == null) {
                throw new IllegalArgumentException("typeName cannot be null");
            }
            if (title == null) {
                title = "";
            }
        }
    }

    /**
     * Nested stream value from STREAM type atoms.
     * Contains a child FdoStream that can be traversed.
     */
    record StreamValue(FdoStream stream) implements FdoValue {
        public StreamValue {
            if (stream == null) {
                throw new IllegalArgumentException("stream cannot be null");
            }
        }
    }

    /**
     * Raw binary value for unknown or untyped atoms.
     * Preserved as hex string with 'x' suffix (e.g., "deadbeefx").
     */
    record RawValue(byte[] data) implements FdoValue {
        public RawValue {
            if (data == null) {
                data = new byte[0];
            }
            data = data.clone(); // defensive copy
        }

        @Override
        public byte[] data() {
            return data.clone(); // defensive copy on access
        }

        /**
         * Get raw data as hex string with 'x' suffix.
         */
        public String toHexString() {
            if (data.length == 0) {
                return "00x";
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : data) {
                hex.append(String.format("%02x", b & 0xFF));
            }
            hex.append('x');
            return hex.toString();
        }
    }

    /**
     * List of values for atoms with multiple arguments.
     * Used for comma-separated lists like mat_size or var_number_set.
     */
    record ListValue(List<FdoValue> elements) implements FdoValue {
        public ListValue {
            if (elements == null) {
                throw new IllegalArgumentException("elements cannot be null");
            }
            elements = List.copyOf(elements); // immutable copy
        }
    }

    /**
     * Empty value for atoms with no data.
     */
    record EmptyValue() implements FdoValue {
        private static final EmptyValue INSTANCE = new EmptyValue();

        public static EmptyValue instance() {
            return INSTANCE;
        }
    }

    // Convenience type-checking methods

    /**
     * Check if this is a string value.
     */
    default boolean isString() {
        return this instanceof StringValue;
    }

    /**
     * Check if this is a numeric value.
     */
    default boolean isNumber() {
        return this instanceof NumberValue;
    }

    /**
     * Check if this is a GID value.
     */
    default boolean isGid() {
        return this instanceof GidValue;
    }

    /**
     * Check if this is a boolean value.
     */
    default boolean isBoolean() {
        return this instanceof BooleanValue;
    }

    /**
     * Check if this is a nested stream value.
     */
    default boolean isStream() {
        return this instanceof StreamValue;
    }

    /**
     * Check if this is an empty value.
     */
    default boolean isEmpty() {
        return this instanceof EmptyValue;
    }

    /**
     * Check if this is a list value.
     */
    default boolean isList() {
        return this instanceof ListValue;
    }
}
