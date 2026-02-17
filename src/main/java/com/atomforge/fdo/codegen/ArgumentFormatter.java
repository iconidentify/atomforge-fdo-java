package com.atomforge.fdo.codegen;

import com.atomforge.fdo.text.ast.ArgumentNode;
import com.atomforge.fdo.text.ast.ArgumentNode.*;
import com.atomforge.fdo.text.ast.StreamNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats ArgumentNode objects to Java DSL argument strings.
 *
 * The key insight is that arguments are emitted as strings that DslTextEmitter
 * will pass through unchanged. This ensures text-level compatibility and
 * guarantees byte-identical binary output.
 */
public final class ArgumentFormatter {

    private ArgumentFormatter() {} // utility class

    /**
     * Format an argument node to its Java representation.
     *
     * @param arg The argument node from the AST
     * @param generator The generator (for recursive nested stream handling)
     * @return The Java argument string
     */
    public static String format(ArgumentNode arg, DslCodeGenerator generator) {
        return switch (arg) {
            case StringArg sa -> formatString(sa.value());
            case NumberArg na -> formatNumber(na.value());
            case HexArg ha -> formatHex(ha.value());
            case GidArg ga -> formatGid(ga.value());
            case IdentifierArg ia -> formatIdentifier(ia.value());
            case PipedArg pa -> formatPiped(pa.parts(), generator);
            case ListArg la -> formatList(la.elements(), generator);
            case ObjectTypeArg ota -> formatObjectType(ota.objectType(), ota.title());
            case NestedStreamArg nsa -> formatNestedStream(nsa.stream(), nsa.trailingData(), generator);
        };
    }

    /**
     * Format all arguments for an atom call.
     *
     * @param args The argument nodes
     * @param generator The generator for recursive handling
     * @return List of formatted argument strings
     */
    public static List<String> formatAll(List<ArgumentNode> args, DslCodeGenerator generator) {
        List<String> formatted = new ArrayList<>();
        for (ArgumentNode arg : args) {
            formatted.add(format(arg, generator));
        }
        return formatted;
    }

    // ========== Type-specific formatters ==========

    /**
     * Format a string literal: "Hello" -> "\"Hello\""
     */
    private static String formatString(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Format a number: 512 -> "512"
     */
    private static String formatNumber(long value) {
        return String.valueOf(value);
    }

    /**
     * Format hex: 00x -> "\"00x\""
     */
    private static String formatHex(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Format GID: 32-117 -> "\"32-117\""
     */
    private static String formatGid(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Format identifier: vcf -> "\"vcf\""
     */
    private static String formatIdentifier(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Format piped: [left, center] -> "\"left | center\""
     */
    private static String formatPiped(List<ArgumentNode> parts, DslCodeGenerator generator) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" | ");
            ArgumentNode part = parts.get(i);
            // For piped args, inner parts are identifiers or numbers
            if (part instanceof IdentifierArg ia) {
                sb.append(ia.value());
            } else if (part instanceof NumberArg na) {
                sb.append(na.value());
            } else if (part instanceof StringArg sa) {
                sb.append(sa.value());
            } else {
                // Fallback: strip quotes from formatted output
                String formatted = format(part, generator);
                if (formatted.startsWith("\"") && formatted.endsWith("\"")) {
                    sb.append(formatted.substring(1, formatted.length() - 1));
                } else {
                    sb.append(formatted);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Format list: [50, 4, 512] -> "50, 4, 512" (multiple args)
     * Returns a comma-separated list that will be expanded into multiple args.
     */
    private static String formatList(List<ArgumentNode> elements, DslCodeGenerator generator) {
        // Lists expand into multiple arguments
        return elements.stream()
                .map(e -> format(e, generator))
                .collect(Collectors.joining(", "));
    }

    /**
     * Format object type: (ind_group, "Title") -> "\"ind_group\", \"Title\""
     */
    private static String formatObjectType(String objectType, String title) {
        if (title == null || title.isEmpty()) {
            return "\"" + objectType + "\"";
        }
        return "\"" + objectType + "\", " + formatString(title);
    }

    /**
     * Format nested stream with lambda syntax.
     */
    private static String formatNestedStream(StreamNode stream, List<ArgumentNode> trailingData,
                                              DslCodeGenerator generator) {
        // This is handled specially in DslCodeGenerator since it needs multi-line output
        // Return a placeholder that the generator will recognize
        return "NESTED_STREAM";
    }

    /**
     * Check if an argument is a nested stream.
     */
    public static boolean isNestedStream(ArgumentNode arg) {
        return arg instanceof NestedStreamArg;
    }

    /**
     * Get the nested stream from an argument.
     */
    public static NestedStreamArg asNestedStream(ArgumentNode arg) {
        return (NestedStreamArg) arg;
    }

    /**
     * Check if an argument is a list.
     */
    public static boolean isList(ArgumentNode arg) {
        return arg instanceof ListArg;
    }

    /**
     * Get list elements from an argument.
     */
    public static List<ArgumentNode> getListElements(ArgumentNode arg) {
        if (arg instanceof ListArg la) {
            return la.elements();
        }
        return List.of(arg);
    }
}
