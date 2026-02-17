package com.atomforge.fdo.dsl.internal;

import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.dsl.atoms.DslAtom;
import com.atomforge.fdo.dsl.atoms.ManAtom;
import com.atomforge.fdo.dsl.values.*;
import com.atomforge.fdo.model.FdoGid;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts DSL frames to FDO text source.
 *
 * This allows the DSL to delegate binary encoding to the proven FdoCompiler,
 * eliminating duplicate encoding logic and ensuring byte-for-byte compatibility
 * with the text compiler path.
 *
 * <p><strong>Type-Safety:</strong> This emitter uses the atom's {@link AtomType}
 * to determine how to format arguments, rather than guessing from string content.
 * This ensures that:
 * <ul>
 *   <li>STRING type atoms always quote their string arguments</li>
 *   <li>GID type atoms emit FdoGid objects in proper format</li>
 *   <li>OBJSTART atoms emit object type and quoted title</li>
 * </ul>
 *
 * <p><strong>Atom-Specific Overrides:</strong> Some atoms require quoting behavior
 * that differs from their AtomType default. These are handled via {@link #getQuotingOverride(String)}.
 */
public final class DslTextEmitter {

    private DslTextEmitter() {}

    /** Controls whether string arguments should be quoted or not. */
    private enum QuoteMode { QUOTE, UNQUOTE }

    /**
     * Get atom-specific quoting override.
     * Some atoms have quoting requirements that differ from their AtomType default.
     *
     * @param atomName The atom name
     * @return QuoteMode override, or null to use type-based logic
     */
    private static QuoteMode getQuotingOverride(String atomName) {
        return switch (atomName) {
            // Atoms that MUST quote their string args (override RAW/TOKEN defaults)
            case "chat_add_user",
                 "sm_send_token_raw",
                 "sm_send_token_arg",
                 "buf_set_token",
                 "vid_set_token" -> QuoteMode.QUOTE;

            // Atoms that MUST NOT quote their string args (override STRING/TOKENARG defaults)
            case "act_set_criterion",
                 "act_do_action",
                 "uni_use_last_atom_string",
                 "uni_use_last_atom_value",
                 "de_validate" -> QuoteMode.UNQUOTE;

            default -> null;  // Use type-based logic
        };
    }

    /**
     * Emit FDO text source from a list of DSL frames.
     *
     * @param frames The frames to emit
     * @return FDO text source that can be compiled by FdoCompiler
     */
    public static String emit(List<DslAtomFrame> frames) {
        // Optimize: remove redundant man_end_object before man_start_sibling
        List<DslAtomFrame> optimized = optimizeSiblingFrames(frames);

        StringBuilder sb = new StringBuilder();
        for (DslAtomFrame frame : optimized) {
            emitFrame(frame, sb);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Optimize frame list by removing redundant man_end_object atoms.
     *
     * In FDO, man_start_sibling implicitly ends the previous object, so an
     * explicit man_end_object immediately before man_start_sibling is redundant.
     * The scoped DSL API produces this pattern because object() always emits
     * man_end_object in its finally block.
     *
     * @param frames The original frame list
     * @return Optimized frame list with redundant END_OBJECT atoms removed
     */
    private static List<DslAtomFrame> optimizeSiblingFrames(List<DslAtomFrame> frames) {
        List<DslAtomFrame> result = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            DslAtomFrame current = frames.get(i);

            // Skip man_end_object if next frame is man_start_sibling
            if (isEndObject(current) && i + 1 < frames.size()) {
                DslAtomFrame next = frames.get(i + 1);
                if (isStartSibling(next)) {
                    continue; // Skip this redundant END_OBJECT
                }
            }
            result.add(current);
        }
        return result;
    }

    private static boolean isEndObject(DslAtomFrame frame) {
        return frame.atom() == ManAtom.END_OBJECT;
    }

    private static boolean isStartSibling(DslAtomFrame frame) {
        return frame.atom() == ManAtom.START_SIBLING;
    }

    private static void emitFrame(DslAtomFrame frame, StringBuilder sb) {
        DslAtom atom = frame.atom();
        AtomType atomType = atom.type();

        // Emit atom name
        sb.append(atom.atomName());

        List<Object> args = frame.args();
        boolean hasNestedStream = frame.hasNestedStream();

        // Only emit args/nested stream if there are any
        if (!args.isEmpty() || hasNestedStream) {
            sb.append(" <");

            // Emit regular arguments based on atom type and name
            String atomName = atom.atomName();
            emitArgs(args, atomType, atomName, sb);

            // Emit nested stream if present
            if (hasNestedStream) {
                if (!args.isEmpty()) sb.append(", ");
                emitNestedStream(frame.nestedStream(), sb);
            }

            sb.append(">");
        } else if (atomType == AtomType.BOOL) {
            // BOOL atoms with no args default to "yes"
            sb.append(" <yes>");
        }
    }

    /**
     * Emit arguments based on the atom's type and name.
     * The AtomType tells us how the compiler expects to parse the arguments,
     * but some atoms need special quoting overrides.
     */
    private static void emitArgs(List<Object> args, AtomType atomType, String atomName, StringBuilder sb) {
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            emitTypedArg(args.get(i), atomType, atomName, i, sb);
        }
    }

    /**
     * Emit a single argument with knowledge of the expected atom type.
     *
     * @param arg The argument value
     * @param atomType The atom's declared type (tells us how to format)
     * @param atomName The atom's name (for special quoting overrides)
     * @param argIndex The argument index (some types have multiple args with different formats)
     * @param sb The output buffer
     */
    private static void emitTypedArg(Object arg, AtomType atomType, String atomName, int argIndex, StringBuilder sb) {
        if (arg == null) {
            sb.append("\"\"");
            return;
        }

        // First, check for type-safe DSL value objects - these always emit the same way
        if (arg instanceof FdoGid gid) {
            sb.append(gid.toString());
            return;
        }
        if (arg instanceof ObjectType type) {
            sb.append(type.fdoName());
            return;
        }
        if (arg instanceof Orientation orient) {
            sb.append(orient.fdoName());
            return;
        }
        if (arg instanceof Position pos) {
            sb.append(pos.fdoName());
            return;
        }
        if (arg instanceof TitlePosition tp) {
            sb.append(tp.fdoName());
            return;
        }
        if (arg instanceof FontId font) {
            sb.append(font.fdoName());
            return;
        }
        if (arg instanceof FontStyle style) {
            sb.append(style.fdoName());
            return;
        }
        if (arg instanceof FrameStyle style) {
            sb.append(style.fdoName());
            return;
        }
        if (arg instanceof TriggerStyle style) {
            sb.append(style.fdoName());
            return;
        }
        if (arg instanceof LogType log) {
            sb.append(log.fdoName());
            return;
        }
        if (arg instanceof SortOrder order) {
            sb.append(order.fdoName());
            return;
        }
        if (arg instanceof Criterion crit) {
            sb.append(crit.fdoName());
            return;
        }
        if (arg instanceof CriterionArg ca) {
            sb.append(ca.fdoName());
            return;
        }
        if (arg instanceof DataType dt) {
            sb.append(dt.fdoName());
            return;
        }
        if (arg instanceof byte[] bytes) {
            emitBytes(bytes, sb);
            return;
        }
        if (arg instanceof DslAtom atom) {
            // Atom reference (e.g., for uni_use_last_atom_string)
            sb.append(atom.atomName());
            return;
        }
        if (arg instanceof Boolean b) {
            sb.append(b ? "yes" : "no");
            return;
        }
        if (arg instanceof Number n) {
            sb.append(n);
            return;
        }

        // For String arguments, use the atom type and name to determine quoting
        if (arg instanceof String s) {
            emitStringByAtomType(s, atomType, atomName, argIndex, sb);
            return;
        }

        // Fallback - use toString and quote it to be safe
        emitQuotedString(arg.toString(), sb);
    }

    /**
     * Emit a string argument based on the atom's type and name.
     * Checks for atom-specific overrides first, then falls back to type-based logic.
     */
    private static void emitStringByAtomType(String s, AtomType atomType, String atomName, int argIndex, StringBuilder sb) {
        // Check for atom-specific quoting override first
        QuoteMode override = getQuotingOverride(atomName);
        if (override == QuoteMode.QUOTE) {
            emitQuotedString(s, sb);
            return;
        }
        if (override == QuoteMode.UNQUOTE) {
            sb.append(s);
            return;
        }

        // Fall back to type-based logic
        switch (atomType) {
            case STRING:
            case TOKENARG:
            case VARSTRING:
                // STRING type atoms: always quote the string
                emitQuotedString(s, sb);
                break;

            case OBJSTART:
                // OBJSTART: first arg is object type (unquoted), second is title (quoted)
                if (argIndex == 0) {
                    // Object type name - unquoted keyword
                    sb.append(s);
                } else {
                    // Title - always quoted
                    emitQuotedString(s, sb);
                }
                break;

            case GID:
                // GID type: should have been passed as FdoGid, but if string, emit as-is
                // This is a style_id like "32-117" which is valid unquoted GID syntax
                sb.append(s);
                break;

            case RAW:
                // RAW: emit as-is (typically hex with x suffix)
                sb.append(s);
                break;

            case BOOL:
            case CRITERION:
                // Boolean keywords: yes/no/true/false - unquoted
                sb.append(s);
                break;

            case TOKEN:
                // Token strings: pipe-separated action names - emit unquoted
                sb.append(s);
                break;

            case ORIENT:
                // Orientation codes like vcf, hef - unquoted
                sb.append(s);
                break;

            case ATOM:
                // Atom reference name - unquoted
                sb.append(s);
                break;

            case DWORD:
            case VAR:
            case VARDWORD:
            case VARLOOKUP:
                // Numeric or variable references - unquoted
                sb.append(s);
                break;

            default:
                // Unknown type - quote to be safe
                emitQuotedString(s, sb);
                break;
        }
    }

    /**
     * Emit a properly quoted and escaped string.
     */
    private static void emitQuotedString(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
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
    }

    private static void emitBytes(byte[] bytes, StringBuilder sb) {
        // Emit as hex string with x suffix
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        sb.append("x");
    }

    private static void emitNestedStream(List<DslAtomFrame> frames, StringBuilder sb) {
        // Nested streams are wrapped in a stream block
        sb.append("\n");
        for (DslAtomFrame frame : frames) {
            emitFrame(frame, sb);
            sb.append("\n");
        }
    }
}
