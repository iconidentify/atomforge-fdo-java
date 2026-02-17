package com.atomforge.fdo.codegen;

import com.atomforge.fdo.text.ast.ArgumentNode;

import java.util.List;

/**
 * Maps atom names to their corresponding typed method names in StreamBuilder.
 *
 * This enables DslCodeGenerator to emit typed method calls (e.g., .manStartObject())
 * instead of generic .atom() calls.
 */
public final class TypedMethodMapper {

    private TypedMethodMapper() {} // utility class

    /**
     * Get the typed method name for an atom.
     *
     * @param atomName The atom name (e.g., "man_start_object")
     * @param args The arguments for the atom
     * @return The method name (e.g., "manStartObject"), or null if no typed method exists
     */
    public static String getTypedMethodName(String atomName, List<ArgumentNode> args) {
        if (atomName == null) {
            return null;
        }

        // Extract protocol prefix and atom name
        String[] parts = atomName.split("_", 2);
        if (parts.length != 2) {
            return null;
        }

        String protocol = parts[0];
        String atomPart = parts[1];

        // Convert atom name to camelCase method name
        String methodName = toCamelCase(atomPart);

        // Handle special cases and overloads
        String fullMethodName = protocol + capitalize(methodName);

        // Check for overloads based on argument count/type
        return handleOverloads(fullMethodName, args);
    }

    /**
     * Convert snake_case to camelCase.
     */
    private static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    /**
     * Capitalize first letter.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Handle method overloads based on argument count and types.
     */
    private static String handleOverloads(String baseMethodName, List<ArgumentNode> args) {
        int argCount = args.size();

        // Handle special overload cases
        // For example, manStartObject() vs manStartObject(ObjectType, String)
        if (baseMethodName.equals("manStartObject")) {
            if (argCount == 0) {
                return null; // No no-arg version
            } else if (argCount == 1) {
                return "manStartObject"; // manStartObject(ObjectType)
            } else {
                return "manStartObject"; // manStartObject(ObjectType, String)
            }
        }

        if (baseMethodName.equals("manStartSibling")) {
            if (argCount == 0) {
                return null;
            } else if (argCount == 1) {
                return "manStartSibling"; // manStartSibling(ObjectType)
            } else {
                return "manStartSibling"; // manStartSibling(ObjectType, String)
            }
        }

        if (baseMethodName.equals("uniStartStream")) {
            if (argCount == 0) {
                return "uniStartStream"; // uniStartStream()
            } else {
                return "uniStartStream"; // uniStartStream(String)
            }
        }

        if (baseMethodName.equals("uniEndStream")) {
            if (argCount == 0) {
                return "uniEndStream"; // uniEndStream()
            } else {
                return "uniEndStream"; // uniEndStream(String)
            }
        }

        if (baseMethodName.equals("matSize")) {
            if (argCount == 2) {
                return "matSize"; // matSize(int, int)
            } else if (argCount == 3) {
                return "matSize"; // matSize(int, int, int)
            }
        }

        // For boolean MAT atoms, there are two overloads:
        // - no-arg version (defaults to yes/true)
        // - boolean arg version (for explicit yes/no)
        if (baseMethodName.startsWith("matBool")) {
            if (argCount == 0) {
                return baseMethodName;
            } else if (argCount == 1) {
                // Both "yes" and "no" use the typed method with boolean overload
                ArgumentNode arg = args.get(0);
                if (arg instanceof ArgumentNode.IdentifierArg ia) {
                    String val = ia.value().toLowerCase();
                    if (val.equals("yes") || val.equals("true") || val.equals("1") ||
                        val.equals("no") || val.equals("false") || val.equals("0")) {
                        return baseMethodName;
                    }
                }
                // For other values, fall back to generic atom() call
                return null;
            }
        }

        // Default: return the base method name
        // The actual method signature will be determined by the argument types
        return baseMethodName;
    }

    /**
     * Check if arguments should be skipped when generating code for this atom.
     * This is used for boolean methods where the "yes"/"no" argument is implicit.
     *
     * @param atomName The atom name
     * @param args The arguments
     * @return true if arguments should be skipped (e.g., matBool methods with "yes")
     */
    public static boolean shouldSkipArguments(String atomName, List<ArgumentNode> args) {
        if (atomName == null || args.isEmpty()) {
            return false;
        }

        String methodName = getTypedMethodName(atomName, args);
        if (methodName != null && methodName.startsWith("matBool") && args.size() == 1) {
            // matBool methods with "yes" argument - skip the argument
            ArgumentNode arg = args.get(0);
            if (arg instanceof ArgumentNode.IdentifierArg ia) {
                String val = ia.value().toLowerCase();
                return val.equals("yes") || val.equals("true") || val.equals("1");
            }
        }
        return false;
    }

    /**
     * Check if a typed method exists for an atom.
     */
    public static boolean hasTypedMethod(String atomName) {
        if (atomName == null) {
            return false;
        }

        // All atoms with protocol prefixes should have typed methods
        String[] parts = atomName.split("_", 2);
        return parts.length == 2;
    }
}

