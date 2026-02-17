package com.atomforge.fdo.atom;

import java.util.EnumSet;

/**
 * Atom formatting flags.
 * These control indentation during decompilation.
 */
public enum AtomFlags {
    /**
     * Increase indentation after this atom
     */
    INDENT(0x01),

    /**
     * Decrease indentation before this atom
     */
    OUTDENT(0x02),

    /**
     * End of stream marker
     */
    EOS(0x04);

    private final int bit;

    AtomFlags(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    /**
     * Parse flag set from combined integer value
     */
    public static EnumSet<AtomFlags> fromBits(int bits) {
        EnumSet<AtomFlags> flags = EnumSet.noneOf(AtomFlags.class);
        for (AtomFlags flag : values()) {
            if ((bits & flag.bit) != 0) {
                flags.add(flag);
            }
        }
        return flags;
    }

    /**
     * Convert flag set to combined integer value
     */
    public static int toBits(EnumSet<AtomFlags> flags) {
        int bits = 0;
        for (AtomFlags flag : flags) {
            bits |= flag.bit;
        }
        return bits;
    }

    /**
     * Parse flags from space-separated directive strings (e.g., "indent", "eos outdent")
     */
    public static EnumSet<AtomFlags> fromDirectives(String directives) {
        EnumSet<AtomFlags> flags = EnumSet.noneOf(AtomFlags.class);
        if (directives == null || directives.isEmpty()) {
            return flags;
        }
        String[] parts = directives.toLowerCase().trim().split("\\s+");
        for (String part : parts) {
            switch (part) {
                case "indent" -> flags.add(INDENT);
                case "outdent" -> flags.add(OUTDENT);
                case "eos" -> flags.add(EOS);
            }
        }
        return flags;
    }
}
