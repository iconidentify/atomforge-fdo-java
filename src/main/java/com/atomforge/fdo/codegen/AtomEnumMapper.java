package com.atomforge.fdo.codegen;

import com.atomforge.fdo.atom.Protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps FDO atom names to DSL enum references.
 *
 * Converts names like "man_start_object" to "ManAtom.START_OBJECT".
 */
public final class AtomEnumMapper {

    private AtomEnumMapper() {} // utility class

    // Map prefix -> enum class simple name
    private static final Map<String, String> PREFIX_TO_ENUM = new HashMap<>();

    static {
        // Build map from Protocol.getPrefix() values to enum class names
        // The enum classes follow pattern: XxxAtom where Xxx is title-cased prefix
        for (int proto = 0; proto <= 55; proto++) {
            String prefix = Protocol.getPrefix(proto);
            if (prefix != null && !prefix.startsWith("proto")) {
                String enumName = toEnumClassName(prefix);
                PREFIX_TO_ENUM.put(prefix, enumName);
            }
        }
    }

    /**
     * Get all known prefixes.
     */
    public static Set<String> knownPrefixes() {
        return PREFIX_TO_ENUM.keySet();
    }

    /**
     * Convert a prefix to the enum class name.
     * e.g., "man" -> "ManAtom", "imgxfer" -> "ImgxferAtom"
     */
    private static String toEnumClassName(String prefix) {
        // Title case the first letter, rest lowercase, add "Atom"
        if (prefix.isEmpty()) return "Atom";
        return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1).toLowerCase() + "Atom";
    }

    /**
     * Map an atom name to its DSL enum reference.
     *
     * @param atomName The FDO atom name (e.g., "man_start_object")
     * @return The Java enum reference (e.g., "ManAtom.START_OBJECT")
     * @throws IllegalArgumentException if the atom name format is unrecognized
     */
    public static String mapToEnumRef(String atomName) {
        if (atomName == null || atomName.isEmpty()) {
            throw new IllegalArgumentException("Atom name cannot be null or empty");
        }

        // Find the prefix by matching known prefixes
        String prefix = null;
        String suffix = null;

        for (String knownPrefix : PREFIX_TO_ENUM.keySet()) {
            String prefixWithUnderscore = knownPrefix + "_";
            if (atomName.startsWith(prefixWithUnderscore)) {
                // Take the longest matching prefix
                if (prefix == null || knownPrefix.length() > prefix.length()) {
                    prefix = knownPrefix;
                    suffix = atomName.substring(prefixWithUnderscore.length());
                }
            }
        }

        if (prefix == null) {
            throw new IllegalArgumentException("Unknown atom prefix in: " + atomName);
        }

        String enumClass = PREFIX_TO_ENUM.get(prefix);
        String enumConstant = suffix.toUpperCase();

        return enumClass + "." + enumConstant;
    }

    /**
     * Get the enum class name for a protocol prefix.
     *
     * @param prefix The protocol prefix (e.g., "man", "uni")
     * @return The enum class name (e.g., "ManAtom", "UniAtom")
     */
    public static String getEnumClassName(String prefix) {
        return PREFIX_TO_ENUM.get(prefix.toLowerCase());
    }

    /**
     * Get the import statement for a protocol prefix.
     *
     * @param prefix The protocol prefix (e.g., "man", "uni")
     * @return The import statement for the enum class
     */
    public static String getImportStatement(String prefix) {
        String enumName = getEnumClassName(prefix);
        if (enumName == null) return null;
        return "import com.atomforge.fdo.dsl.atoms." + enumName + ";";
    }

    /**
     * Extract the protocol prefix from an atom name.
     *
     * @param atomName The FDO atom name (e.g., "man_start_object")
     * @return The prefix (e.g., "man"), or null if not found
     */
    public static String extractPrefix(String atomName) {
        if (atomName == null || atomName.isEmpty()) {
            return null;
        }

        for (String knownPrefix : PREFIX_TO_ENUM.keySet()) {
            String prefixWithUnderscore = knownPrefix + "_";
            if (atomName.startsWith(prefixWithUnderscore)) {
                return knownPrefix;
            }
        }
        return null;
    }
}
