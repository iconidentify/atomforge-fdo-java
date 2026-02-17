package com.atomforge.fdo.binary;

/**
 * Binary encoding styles for atom streams.
 * Each style provides different compression for different scenarios.
 */
public enum EncodingStyle {
    /**
     * Full format: [style|proto] [atom] [len] [data...]
     * Used for general case with any protocol, atom, and data size.
     */
    FULL(0),

    /**
     * Length format: [style|proto] [len|atom] [data...]
     * Used when data is 1-7 bytes and atom < 32.
     */
    LENGTH(1),

    /**
     * Data format: [style|proto] [data|atom]
     * Used when data is single byte 0-7 and atom < 32.
     */
    DATA(2),

    /**
     * Atom-only format: [style|atom]
     * Used when there is no data and atom < 32.
     */
    ATOM(3),

    /**
     * Current protocol format: [style|atom] [len] [data...]
     * Used when protocol matches current context.
     */
    CURRENT(4),

    /**
     * Zero format: [style|atom]
     * Used when data is implicit single byte 0 and atom < 32.
     */
    ZERO(5),

    /**
     * One format: [style|atom]
     * Used when data is implicit single byte 1 and atom < 32.
     */
    ONE(6),

    /**
     * Prefix format: [111PPAAS] [style|proto] [atom] ...
     * Used for extended protocols (32-127).
     */
    PREFIX(7);

    public static final int STYLE_SHIFT = 5;
    public static final int STYLE_MASK = 0x1F;
    public static final int MAX_INLINE_ATOM = 31;
    public static final int MAX_INLINE_DATA_LENGTH = 7;
    public static final int MAX_SINGLE_BYTE_LENGTH = 127;  // High bit clear = single byte

    private final int code;

    EncodingStyle(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Extract style from first byte of encoded atom.
     */
    public static EncodingStyle fromByte(int firstByte) {
        int styleCode = (firstByte >> STYLE_SHIFT) & 0x07;
        return switch (styleCode) {
            case 0 -> FULL;
            case 1 -> LENGTH;
            case 2 -> DATA;
            case 3 -> ATOM;
            case 4 -> CURRENT;
            case 5 -> ZERO;
            case 6 -> ONE;
            case 7 -> PREFIX;
            default -> throw new IllegalArgumentException("Invalid style code: " + styleCode);
        };
    }

    /**
     * Create the first byte combining style and low 5 bits of value.
     */
    public int encodeFirstByte(int lowBits) {
        return (code << STYLE_SHIFT) | (lowBits & STYLE_MASK);
    }

    /**
     * Check if this style can encode the given atom number (must be < 32).
     */
    public static boolean canUseCompactAtom(int atomNumber) {
        return atomNumber >= 0 && atomNumber <= MAX_INLINE_ATOM;
    }
}
