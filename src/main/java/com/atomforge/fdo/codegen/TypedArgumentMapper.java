package com.atomforge.fdo.codegen;

import com.atomforge.fdo.dsl.values.*;

import java.util.Set;

/**
 * Maps atom arguments to their typed enum representations for code generation.
 *
 * This enables the code generator to emit strongly-typed enum references
 * instead of raw strings when the argument value matches a known enum.
 *
 * <p>Example: For "act_set_criterion <close>", instead of generating:
 * <pre>.atom(ActAtom.SET_CRITERION, "close")</pre>
 *
 * It generates:
 * <pre>.atom(ActAtom.SET_CRITERION, Criterion.CLOSE)</pre>
 */
public final class TypedArgumentMapper {

    private TypedArgumentMapper() {} // utility class

    /**
     * Atoms whose first argument should be a Criterion enum.
     */
    private static final Set<String> CRITERION_ATOMS = Set.of(
        "act_set_criterion",
        "act_do_action"
    );

    /**
     * Atoms whose first argument should be an ObjectType enum.
     */
    private static final Set<String> OBJECT_TYPE_ATOMS = Set.of(
        "man_start_object",
        "man_start_sibling"
    );

    /**
     * Atoms whose first argument should be an Orientation enum.
     */
    private static final Set<String> ORIENTATION_ATOMS = Set.of(
        "mat_orientation"
    );

    /**
     * Atoms whose first argument should be a Position enum.
     */
    private static final Set<String> POSITION_ATOMS = Set.of(
        "mat_position"
    );

    /**
     * Atoms whose first argument should be a FontId enum.
     */
    private static final Set<String> FONT_ID_ATOMS = Set.of(
        "mat_font_id"
    );

    /**
     * Atoms whose first argument should be a FontStyle enum.
     */
    private static final Set<String> FONT_STYLE_ATOMS = Set.of(
        "mat_font_style"
    );

    /**
     * Atoms whose first argument should be a TitlePosition enum.
     */
    private static final Set<String> TITLE_POSITION_ATOMS = Set.of(
        "mat_title_pos"
    );

    /**
     * Atoms whose first argument should be a LogType enum.
     */
    private static final Set<String> LOG_TYPE_ATOMS = Set.of(
        "mat_log_object"
    );

    /**
     * Atoms whose first argument should be a SortOrder enum.
     */
    private static final Set<String> SORT_ORDER_ATOMS = Set.of(
        "mat_sort_order"
    );

    /**
     * Atoms whose first argument should be a TriggerStyle enum.
     */
    private static final Set<String> TRIGGER_STYLE_ATOMS = Set.of(
        "mat_trigger_style"
    );

    /**
     * Atoms whose first argument should be a DslAtom reference.
     */
    private static final Set<String> ATOM_REF_ATOMS = Set.of(
        "uni_use_last_atom_string",
        "uni_use_last_atom_value"
    );

    /**
     * Atoms with FontId in first position and FontStyle in third position (mat_font_sis).
     */
    private static final Set<String> FONT_SIS_ATOMS = Set.of(
        "mat_font_sis"
    );

    /**
     * Atoms that take yes/no as boolean-like integer arguments (yes=1, no=0).
     */
    private static final Set<String> BOOLEAN_INT_ATOMS = Set.of(
        "mat_paragraph",
        "mat_scroll_threshold"  // Some atoms that can take yes/no in FDO but expect int in DSL
    );

    /**
     * Atoms whose first argument should be an FdoGid.
     */
    private static final Set<String> GID_ATOMS = Set.of(
        "mat_object_id",
        "mat_art_id",
        "mat_art_frame",
        "mat_form_icon",
        "mat_style_id",
        "mat_managed_by",
        "mat_factory_id",
        "mat_navarrow_art",
        "uni_invoke_local",
        "uni_invoke_no_context",
        "uni_invoke_local_preserve",
        "uni_invoke_local_later",
        "man_close",
        "man_set_context_globalid",
        "man_preset_gid",
        "man_preset_authoring_form",
        "sm_send_k1",
        "sm_send_free_k1",
        "sm_send_paid_k1",
        "sm_send_f1",
        "sm_send_free_f1",
        "sm_send_paid_f1",
        "sm_send_er",
        "sm_send_mr",
        "sm_send_mf",
        "sm_send_bm",
        "sm_send_bn",
        "sm_set_object_domain",
        "sm_idb_get_data",
        "idb_get_data",
        "idb_set_data",
        "idb_start_obj",
        "idb_delete_obj",
        "idb_exists",
        "idb_start_extraction",
        "idb_get_value",
        "idb_append_data",
        "idb_change_context",
        "idb_get_string",
        "idb_set_context",
        "idb_atr_dod"
    );

    /**
     * Try to format an identifier argument as a typed enum reference.
     *
     * @param atomName The atom name (e.g., "act_set_criterion")
     * @param argIndex The argument index (0-based)
     * @param value The argument value (e.g., "close")
     * @return The typed enum reference (e.g., "Criterion.CLOSE"), or null if not typed
     */
    public static String tryFormatTypedArg(String atomName, int argIndex, String value) {
        // matBool atoms - convert yes/no to boolean literals
        if (atomName != null && atomName.startsWith("mat_bool_") && argIndex == 0) {
            String lower = value.toLowerCase();
            if (lower.equals("yes") || lower.equals("true") || lower.equals("1")) {
                return "true";
            } else if (lower.equals("no") || lower.equals("false") || lower.equals("0")) {
                return "false";
            }
            return null;
        }

        // Most typed atoms only care about the first argument
        // Exception: OBJECT_TYPE_ATOMS (title is arg 1), FONT_SIS_ATOMS (FontStyle is arg 2)
        if (argIndex > 0 && !OBJECT_TYPE_ATOMS.contains(atomName) && !FONT_SIS_ATOMS.contains(atomName)) {
            return null;
        }

        // Criterion atoms
        if (CRITERION_ATOMS.contains(atomName) && argIndex == 0) {
            Criterion crit = Criterion.fromName(value);
            if (crit != null) {
                return "Criterion." + crit.name();
            }
            // Check for numeric criterion codes
            try {
                int code = Integer.parseInt(value);
                crit = Criterion.fromCode(code);
                if (crit != null) {
                    return "Criterion." + crit.name();
                }
            } catch (NumberFormatException ignored) {}
            return null;
        }

        // ObjectType atoms - first arg is object type
        if (OBJECT_TYPE_ATOMS.contains(atomName) && argIndex == 0) {
            ObjectType type = ObjectType.fromName(value);
            if (type != null) {
                return "ObjectType." + type.name();
            }
            return null;
        }

        // Orientation atoms
        if (ORIENTATION_ATOMS.contains(atomName) && argIndex == 0) {
            Orientation orient = Orientation.fromName(value);
            if (orient != null) {
                return "Orientation." + orient.name();
            }
            return null;
        }

        // Position atoms
        if (POSITION_ATOMS.contains(atomName) && argIndex == 0) {
            Position pos = Position.fromName(value);
            if (pos != null) {
                return "Position." + pos.name();
            }
            return null;
        }

        // FontId atoms
        if (FONT_ID_ATOMS.contains(atomName) && argIndex == 0) {
            FontId font = FontId.fromName(value);
            if (font != null) {
                return "FontId." + font.name();
            }
            return null;
        }

        // FontStyle atoms
        if (FONT_STYLE_ATOMS.contains(atomName) && argIndex == 0) {
            FontStyle style = FontStyle.fromName(value);
            if (style != null) {
                return "FontStyle." + style.name();
            }
            return null;
        }

        // TitlePosition atoms
        if (TITLE_POSITION_ATOMS.contains(atomName) && argIndex == 0) {
            TitlePosition tp = TitlePosition.fromName(value);
            if (tp != null) {
                return "TitlePosition." + tp.name();
            }
            return null;
        }

        // LogType atoms
        if (LOG_TYPE_ATOMS.contains(atomName) && argIndex == 0) {
            LogType lt = LogType.fromName(value);
            if (lt != null) {
                return "LogType." + lt.name();
            }
            return null;
        }

        // SortOrder atoms
        if (SORT_ORDER_ATOMS.contains(atomName) && argIndex == 0) {
            SortOrder so = SortOrder.fromName(value);
            if (so != null) {
                return "SortOrder." + so.name();
            }
            return null;
        }

        // TriggerStyle atoms
        if (TRIGGER_STYLE_ATOMS.contains(atomName) && argIndex == 0) {
            TriggerStyle ts = TriggerStyle.fromName(value);
            if (ts != null) {
                return "TriggerStyle." + ts.name();
            }
            return null;
        }

        // FontSis atoms - arg 0 is FontId (string), arg 2 is FontStyle (string)
        if (FONT_SIS_ATOMS.contains(atomName)) {
            if (argIndex == 0) {
                FontId font = FontId.fromName(value);
                if (font != null) {
                    return "FontId." + font.name();
                }
                return null;
            } else if (argIndex == 2) {
                FontStyle style = FontStyle.fromName(value);
                if (style != null) {
                    return "FontStyle." + style.name();
                }
                return null;
            }
            // argIndex 1 is font size (int), handled elsewhere
        }

        // Atom reference atoms - map to DslAtom enum references
        if (ATOM_REF_ATOMS.contains(atomName) && argIndex == 0) {
            String enumRef = tryMapAtomRef(value);
            if (enumRef != null) {
                return enumRef;
            }
            return null;
        }

        // Boolean-like int atoms - convert yes/no to 1/0
        if (BOOLEAN_INT_ATOMS.contains(atomName) && argIndex == 0) {
            String lower = value.toLowerCase();
            if (lower.equals("yes") || lower.equals("true") || lower.equals("1")) {
                return "1";
            } else if (lower.equals("no") || lower.equals("false") || lower.equals("0")) {
                return "0";
            }
            // If it's already a numeric value, return as-is
            try {
                Integer.parseInt(value);
                return value;
            } catch (NumberFormatException ignored) {}
            return null;
        }

        return null;
    }

    /**
     * Try to format a GID argument as an FdoGid.of() call.
     *
     * @param atomName The atom name (e.g., "mat_object_id")
     * @param argIndex The argument index (0-based)
     * @param gidValue The GID value (e.g., "69-420" or "1-69-1329")
     * @return The FdoGid.of() expression, or null if not a GID atom
     */
    public static String tryFormatGidArg(String atomName, int argIndex, String gidValue) {
        if (!GID_ATOMS.contains(atomName) || argIndex != 0) {
            return null;
        }

        // Parse GID value (format: "type-id" or "type-subtype-id")
        String[] parts = gidValue.split("-");
        if (parts.length == 2) {
            // 2-part GID: type-id
            return "FdoGid.of(" + parts[0] + ", " + parts[1] + ")";
        } else if (parts.length == 3) {
            // 3-part GID: type-subtype-id
            return "FdoGid.of(" + parts[0] + ", " + parts[1] + ", " + parts[2] + ")";
        }

        // Invalid GID format, return null to fall back to string
        return null;
    }

    /**
     * Check if an atom takes a GID argument at the specified index.
     */
    public static boolean isGidAtom(String atomName, int argIndex) {
        return GID_ATOMS.contains(atomName) && argIndex == 0;
    }

    /**
     * Try to format a piped argument as a typed enum reference.
     * This handles cases like mat_title_pos <left | center> -> TitlePosition.LEFT_CENTER
     *
     * @param atomName The atom name (e.g., "mat_title_pos")
     * @param argIndex The argument index (0-based)
     * @param pipedValue The piped value string (e.g., "left | center")
     * @return The typed enum reference, or null if not typed
     */
    public static String tryFormatPipedArg(String atomName, int argIndex, String pipedValue) {
        // TitlePosition atoms - piped value is "orientation | justify"
        if (TITLE_POSITION_ATOMS.contains(atomName) && argIndex == 0) {
            // Normalize the piped value by removing spaces around the pipe
            String normalized = pipedValue.replace(" ", "").replace("|", " | ");
            TitlePosition tp = TitlePosition.fromName(normalized.replace(" | ", "|"));
            if (tp != null) {
                return "TitlePosition." + tp.name();
            }
        }
        // Position atoms - piped value is "vertical | horizontal" -> "vertical_horizontal"
        if (POSITION_ATOMS.contains(atomName) && argIndex == 0) {
            // Convert "center | center" to "center_center"
            String normalized = pipedValue.replace(" ", "").replace("|", "_");
            Position pos = Position.fromName(normalized);
            if (pos != null) {
                return "Position." + pos.name();
            }
        }
        return null;
    }

    /**
     * Try to format a numeric argument as a typed enum reference.
     *
     * @param atomName The atom name (e.g., "mat_font_sis")
     * @param argIndex The argument index (0-based)
     * @param numValue The numeric value
     * @return The typed enum reference (e.g., "FontId.COURIER"), or null if not typed
     */
    public static String tryFormatNumericArg(String atomName, int argIndex, long numValue) {
        // FontId atoms - first arg is font id code
        if (FONT_ID_ATOMS.contains(atomName) && argIndex == 0) {
            FontId font = FontId.fromCode((int) numValue);
            if (font != null) {
                return "FontId." + font.name();
            }
            return null;
        }

        // FontStyle atoms - first arg is style code
        if (FONT_STYLE_ATOMS.contains(atomName) && argIndex == 0) {
            FontStyle style = FontStyle.fromCode((int) numValue);
            if (style != null) {
                return "FontStyle." + style.name();
            }
            return null;
        }

        // FontSis atoms - arg 0 is FontId, arg 2 is FontStyle
        if (FONT_SIS_ATOMS.contains(atomName)) {
            if (argIndex == 0) {
                FontId font = FontId.fromCode((int) numValue);
                if (font != null) {
                    return "FontId." + font.name();
                }
            } else if (argIndex == 2) {
                FontStyle style = FontStyle.fromCode((int) numValue);
                if (style != null) {
                    return "FontStyle." + style.name();
                }
            }
            return null;
        }

        return null;
    }

    /**
     * Try to map an atom name to its DSL enum reference.
     *
     * @param atomName The atom name (e.g., "man_replace_data")
     * @return The enum reference (e.g., "ManAtom.REPLACE_DATA"), or null
     */
    private static String tryMapAtomRef(String atomName) {
        try {
            return AtomEnumMapper.mapToEnumRef(atomName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the set of additional imports needed for typed arguments in the AST.
     *
     * @param atomName The atom name
     * @return Set of import statements needed, or empty set
     */
    public static Set<String> getImportsForAtom(String atomName) {
        if (CRITERION_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.Criterion;");
        }
        if (OBJECT_TYPE_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.ObjectType;");
        }
        if (ORIENTATION_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.Orientation;");
        }
        if (POSITION_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.Position;");
        }
        if (FONT_ID_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.FontId;");
        }
        if (FONT_STYLE_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.FontStyle;");
        }
        if (TITLE_POSITION_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.TitlePosition;");
        }
        if (LOG_TYPE_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.LogType;");
        }
        if (SORT_ORDER_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.SortOrder;");
        }
        if (TRIGGER_STYLE_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.dsl.values.TriggerStyle;");
        }
        if (GID_ATOMS.contains(atomName)) {
            return Set.of("import com.atomforge.fdo.model.FdoGid;");
        }
        if (FONT_SIS_ATOMS.contains(atomName)) {
            return Set.of(
                "import com.atomforge.fdo.dsl.values.FontId;",
                "import com.atomforge.fdo.dsl.values.FontStyle;"
            );
        }
        // Atom ref atoms use the existing DslAtom imports (handled by AtomEnumMapper)
        return Set.of();
    }

    /**
     * Check if the given atom has any typed arguments.
     */
    public static boolean hasTypedArguments(String atomName) {
        return CRITERION_ATOMS.contains(atomName)
            || OBJECT_TYPE_ATOMS.contains(atomName)
            || ORIENTATION_ATOMS.contains(atomName)
            || POSITION_ATOMS.contains(atomName)
            || FONT_ID_ATOMS.contains(atomName)
            || FONT_STYLE_ATOMS.contains(atomName)
            || TITLE_POSITION_ATOMS.contains(atomName)
            || LOG_TYPE_ATOMS.contains(atomName)
            || SORT_ORDER_ATOMS.contains(atomName)
            || TRIGGER_STYLE_ATOMS.contains(atomName)
            || ATOM_REF_ATOMS.contains(atomName)
            || GID_ATOMS.contains(atomName)
            || FONT_SIS_ATOMS.contains(atomName);
    }
}
