package com.atomforge.fdo.model;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.atom.AtomTable;

import java.util.List;
import java.util.Optional;

/**
 * Pretty-prints FdoStream/FdoAtom to properly formatted FDO source text.
 *
 * <p>Usage:
 * <pre>
 * FdoStream stream = FdoStream.decode(binaryData);
 * String text = StreamPrettyPrinter.format(stream);
 * </pre>
 *
 * <p>Formatting rules:
 * <ul>
 *   <li>2-space indentation per level</li>
 *   <li>OUTDENT atoms decrease indent before writing</li>
 *   <li>INDENT atoms increase indent after writing</li>
 *   <li>Nested streams use separate lines for angle brackets</li>
 * </ul>
 */
public final class StreamPrettyPrinter {

    private static final String DEFAULT_INDENT = "  ";
    private static final AtomTable ATOM_TABLE = AtomTable.loadDefault();

    private final String indent;
    private final StringBuilder sb;
    private int indentLevel;

    private StreamPrettyPrinter(String indent) {
        this.indent = indent;
        this.sb = new StringBuilder();
        this.indentLevel = 0;
    }

    /**
     * Format an FdoStream to FDO source text with default indentation.
     *
     * @param stream The stream to format
     * @return Formatted FDO source text
     */
    public static String format(FdoStream stream) {
        return format(stream, DEFAULT_INDENT);
    }

    /**
     * Format an FdoStream to FDO source text with custom indentation.
     *
     * @param stream The stream to format
     * @param indent The indentation string (e.g., "  " or "\t")
     * @return Formatted FDO source text
     */
    public static String format(FdoStream stream, String indent) {
        StreamPrettyPrinter printer = new StreamPrettyPrinter(indent);
        printer.formatStream(stream.atoms());
        return printer.sb.toString();
    }

    /**
     * Format a single FdoAtom to FDO source text.
     *
     * @param atom The atom to format
     * @return Formatted atom text (single line, no trailing newline)
     */
    public static String format(FdoAtom atom) {
        StreamPrettyPrinter printer = new StreamPrettyPrinter(DEFAULT_INDENT);
        printer.formatAtomContent(atom);
        return printer.sb.toString();
    }

    /**
     * Format a list of atoms.
     */
    private void formatStream(List<FdoAtom> atoms) {
        for (FdoAtom atom : atoms) {
            formatAtom(atom);
            sb.append("\n");
        }
    }

    /**
     * Format a single atom with indentation handling.
     */
    private void formatAtom(FdoAtom atom) {
        Optional<AtomDefinition> defOpt = ATOM_TABLE.findByProtocolAtom(atom.protocol(), atom.atomNumber());

        // Handle OUTDENT before atom (e.g., uni_end_stream, man_end_object)
        if (defOpt.isPresent() && defOpt.get().flags().contains(AtomFlags.OUTDENT)) {
            indentLevel = Math.max(0, indentLevel - 1);
        }

        // Write indentation
        writeIndent();

        // Write atom content (name and value)
        formatAtomContent(atom);

        // Handle INDENT after atom (e.g., uni_start_stream, man_start_object)
        if (defOpt.isPresent() && defOpt.get().flags().contains(AtomFlags.INDENT)) {
            indentLevel++;
        }
    }

    /**
     * Format atom name and value without indentation.
     */
    private void formatAtomContent(FdoAtom atom) {
        sb.append(atom.name());

        // Check if value needs formatting
        if (atom.value() instanceof FdoValue.EmptyValue) {
            // No arguments
            return;
        }

        // Check for nested stream (special multi-line formatting)
        if (atom.value() instanceof FdoValue.StreamValue sv) {
            formatNestedStreamValue(sv);
            return;
        }

        // Regular arguments
        sb.append(" <");
        formatValue(atom.value());
        sb.append(">");
    }

    /**
     * Format a value appropriately.
     */
    private void formatValue(FdoValue value) {
        switch (value) {
            case FdoValue.StringValue sv -> formatString(sv.value());
            case FdoValue.NumberValue nv -> sb.append(nv.value());
            case FdoValue.GidValue gv -> sb.append(gv.gid().toString());
            case FdoValue.BooleanValue bv -> sb.append(bv.value() ? "yes" : "no");
            case FdoValue.OrientationValue ov -> sb.append(ov.code());
            case FdoValue.ObjectTypeValue otv -> formatObjectType(otv);
            case FdoValue.StreamValue sv -> formatNestedStreamValue(sv);
            case FdoValue.RawValue rv -> sb.append(rv.toHexString());
            case FdoValue.ListValue lv -> formatList(lv);
            case FdoValue.EmptyValue ev -> {} // Nothing to output
        }
    }

    /**
     * Format a string value with proper escaping and quotes.
     */
    private void formatString(String s) {
        sb.append("\"");
        // Escape special characters
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c >= 32 && c < 127) {
                        sb.append(c);
                    } else {
                        // Use hex escape for non-printable characters
                        sb.append(String.format("\\x%02x", (int) c));
                    }
                }
            }
        }
        sb.append("\"");
    }

    /**
     * Format an object type value (e.g., ind_group, "Title").
     */
    private void formatObjectType(FdoValue.ObjectTypeValue otv) {
        sb.append(otv.typeName());
        sb.append(", ");
        formatString(otv.title());
    }

    /**
     * Format a list value (comma-separated elements).
     */
    private void formatList(FdoValue.ListValue lv) {
        List<FdoValue> elements = lv.elements();

        // Check if this is a letter+string combo (no space after comma)
        boolean isLetterString = elements.size() == 2
                && elements.get(0) instanceof FdoValue.StringValue sv
                && sv.value().length() == 1
                && Character.isUpperCase(sv.value().charAt(0))
                && elements.get(1) instanceof FdoValue.StringValue;

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(isLetterString ? "," : ", ");
            }
            formatValue(elements.get(i));
        }
    }

    /**
     * Format a nested stream with multi-line brackets.
     *
     * Format:
     * <pre>
     * atom_name
     *     <
     *     uni_start_stream
     *       nested_atom
     *     uni_end_stream
     *     >
     * </pre>
     */
    private void formatNestedStreamValue(FdoValue.StreamValue sv) {
        FdoStream nested = sv.stream();

        if (nested.isEmpty()) {
            // Empty stream - just write empty brackets on same line
            sb.append(" <>");
            return;
        }

        // Multi-line format for nested streams
        sb.append("\n");
        indentLevel++;

        // Opening bracket on its own line
        writeIndent();
        sb.append("<\n");

        // Format nested atoms
        for (FdoAtom atom : nested.atoms()) {
            formatAtom(atom);
            sb.append("\n");
        }

        // Closing bracket on its own line
        writeIndent();
        sb.append(">");

        indentLevel--;
    }

    /**
     * Write current indentation.
     */
    private void writeIndent() {
        for (int i = 0; i < indentLevel; i++) {
            sb.append(indent);
        }
    }
}
