package com.atomforge.fdo.text.formatter;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.text.ast.*;

import java.util.List;

/**
 * Formats AST nodes back to FDO source text.
 * Handles indentation based on atom flags and proper argument formatting.
 */
public final class FdoFormatter {

    private static final String DEFAULT_INDENT = "  ";

    private final String indent;
    private final StringBuilder sb;
    private int indentLevel;

    public FdoFormatter() {
        this(DEFAULT_INDENT);
    }

    public FdoFormatter(String indent) {
        this.indent = indent;
        this.sb = new StringBuilder();
        this.indentLevel = 0;
    }

    /**
     * Format a stream of atoms to FDO source text.
     */
    public String format(StreamNode stream) {
        sb.setLength(0);
        indentLevel = 0;

        for (AtomNode atom : stream.atoms()) {
            formatAtom(atom);
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Format a single atom.
     */
    private void formatAtom(AtomNode atom) {
        // Handle outdent before atom (e.g., uni_end_stream)
        if (atom.hasDefinition()) {
            AtomDefinition def = atom.definition();
            if (def.flags().contains(AtomFlags.OUTDENT)) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
        }

        // Write indentation
        writeIndent();

        // Write atom name
        sb.append(atom.name());

        // Check if atom has INDENT flag - for nested streams, apply indent BEFORE formatting
        boolean hasIndentFlag = atom.hasDefinition() && atom.definition().flags().contains(AtomFlags.INDENT);

        // Write arguments if present
        if (atom.hasArguments()) {
            List<ArgumentNode> args = atom.arguments();
            boolean isNestedStream = args.size() == 1 && args.get(0) instanceof ArgumentNode.NestedStreamArg;

            // For nested streams with INDENT flag, apply indent before the < bracket
            if (isNestedStream && hasIndentFlag) {
                indentLevel++;
                hasIndentFlag = false;  // Don't apply again after
            }

            if (!isNestedStream) {
                sb.append(" ");
            }
            formatArguments(args);
        }

        // Handle indent after atom (for atoms without nested stream arguments)
        if (hasIndentFlag) {
            indentLevel++;
        }
    }

    /**
     * Format arguments within angle brackets.
     */
    private void formatArguments(List<ArgumentNode> arguments) {
        if (arguments.isEmpty()) {
            sb.append("<>");
            return;
        }

        // Single argument
        if (arguments.size() == 1) {
            ArgumentNode arg = arguments.get(0);

            // Check if it's a nested stream that needs multi-line formatting
            if (arg instanceof ArgumentNode.NestedStreamArg nested) {
                // Nested streams have < and > on separate lines
                sb.append("\n");
                indentLevel++;
                writeIndent();
                sb.append("<\n");
                // Don't add extra indent here - formatNestedStream will handle it
                // based on uni_start_stream/uni_end_stream pairs
                formatNestedStream(nested);
                writeIndent();
                sb.append(">");
                indentLevel--;
            } else {
                sb.append("<");
                formatArgument(arg);
                sb.append(">");
            }
        } else {
            // Multiple arguments - comma separated
            sb.append("<");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                formatArgument(arguments.get(i));
            }
            sb.append(">");
        }
    }

    /**
     * Format a single argument value.
     */
    private void formatArgument(ArgumentNode arg) {
        switch (arg) {
            case ArgumentNode.StringArg s -> {
                sb.append("\"");
                // String values from decompiler are already escaped, don't double-escape
                sb.append(s.value());
                sb.append("\"");
            }
            case ArgumentNode.NumberArg n -> sb.append(n.value());
            case ArgumentNode.HexArg h -> sb.append(h.value());
            case ArgumentNode.GidArg g -> sb.append(g.value());
            case ArgumentNode.IdentifierArg id -> sb.append(id.value());
            case ArgumentNode.PipedArg p -> formatPipedArg(p);
            case ArgumentNode.ListArg list -> formatListArg(list);
            case ArgumentNode.ObjectTypeArg obj -> formatObjectTypeArg(obj);
            case ArgumentNode.NestedStreamArg nested -> formatNestedStream(nested);
        }
    }

    /**
     * Format a piped argument (e.g., left | center | 4).
     */
    private void formatPipedArg(ArgumentNode.PipedArg piped) {
        List<ArgumentNode> parts = piped.parts();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            ArgumentNode part = parts.get(i);
            if (part instanceof ArgumentNode.IdentifierArg id) {
                sb.append(id.value());
            } else if (part instanceof ArgumentNode.NumberArg num) {
                sb.append(num.value());
            } else {
                formatArgument(part);
            }
        }
    }

    /**
     * Format a list argument (e.g., 50, 4, 512 or B,"string").
     * Uses ", " separator for numbers, "," for mixed types like letter+string.
     */
    private void formatListArg(ArgumentNode.ListArg list) {
        List<ArgumentNode> elements = list.elements();
        // Check if this is a letter+string combo (no space after comma)
        boolean isLetterString = elements.size() == 2
                && elements.get(0) instanceof ArgumentNode.IdentifierArg id
                && id.value().length() == 1
                && Character.isUpperCase(id.value().charAt(0))
                && elements.get(1) instanceof ArgumentNode.StringArg;

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(isLetterString ? "," : ", ");
            }
            formatArgument(elements.get(i));
        }
    }

    /**
     * Format an object type argument (e.g., ind_group, "Title" or org_group, "").
     * Always includes the title even if empty.
     */
    private void formatObjectTypeArg(ArgumentNode.ObjectTypeArg obj) {
        sb.append(obj.objectType());
        sb.append(", \"");
        if (obj.title() != null) {
            sb.append(escapeString(obj.title()));
        }
        sb.append("\"");
    }

    /**
     * Format a nested stream across multiple lines.
     * Called after the opening < is already written on its own line.
     * Handles uni_start_stream/uni_end_stream/uni_abort_stream block indentation.
     */
    private void formatNestedStream(ArgumentNode.NestedStreamArg nested) {
        StreamNode stream = nested.stream();

        if (stream.isEmpty()) {
            return;
        }

        // Check if we have matching uni_start_stream/uni_end_stream pairs
        List<AtomNode> atoms = stream.atoms();
        boolean hasMatchingStreamPair = hasMatchingStreamPair(atoms);

        // Format atoms with special handling for stream block indentation
        for (AtomNode atom : atoms) {
            String name = atom.name();

            // uni_end_stream or uni_abort_stream decreases indent before printing (if we have matching pair)
            if ((name.equals("uni_end_stream") || name.equals("uni_abort_stream")) && hasMatchingStreamPair) {
                indentLevel--;
            }

            writeIndent();
            formatAtomContent(atom);
            sb.append("\n");

            // uni_start_stream increases indent after printing (if we have matching pair)
            if (name.equals("uni_start_stream") && hasMatchingStreamPair) {
                indentLevel++;
            }
        }
    }

    /**
     * Check if atoms list has matching uni_start_stream/uni_end_stream pairs.
     */
    private boolean hasMatchingStreamPair(List<AtomNode> atoms) {
        boolean hasStart = false;
        boolean hasEnd = false;
        for (AtomNode atom : atoms) {
            if (atom.name().equals("uni_start_stream")) hasStart = true;
            if (atom.name().equals("uni_end_stream")) hasEnd = true;
        }
        return hasStart && hasEnd;
    }

    /**
     * Format just the atom content (name and arguments) without indentation.
     */
    private void formatAtomContent(AtomNode atom) {
        sb.append(atom.name());
        if (atom.arguments() != null && !atom.arguments().isEmpty()) {
            sb.append(" ");
            formatArguments(atom.arguments());
        }
    }

    /**
     * Write current indentation.
     */
    private void writeIndent() {
        for (int i = 0; i < indentLevel; i++) {
            sb.append(indent);
        }
    }

    /**
     * Escape special characters in a string.
     */
    private String escapeString(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Convenience method to format a stream to string.
     */
    public static String formatToString(StreamNode stream) {
        return new FdoFormatter().format(stream);
    }

    /**
     * Convenience method to format a stream with custom indent.
     */
    public static String formatToString(StreamNode stream, String indent) {
        return new FdoFormatter(indent).format(stream);
    }
}
