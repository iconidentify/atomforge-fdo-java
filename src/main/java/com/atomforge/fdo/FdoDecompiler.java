package com.atomforge.fdo;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.BinaryDecoder;
import com.atomforge.fdo.text.ast.*;
import com.atomforge.fdo.text.formatter.FdoFormatter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main facade for decompiling binary FDO to source text.
 *
 * Usage:
 * <pre>
 * FdoDecompiler decompiler = FdoDecompiler.create();
 * String source = decompiler.decompile(binaryData);
 * </pre>
 */
public final class FdoDecompiler {

    private final AtomTable atomTable;
    private final FdoFormatter formatter;
    private final boolean preserveUnknownFormat;

    private FdoDecompiler(AtomTable atomTable, boolean preserveUnknownFormat) {
        this.atomTable = atomTable;
        this.formatter = new FdoFormatter();
        this.preserveUnknownFormat = preserveUnknownFormat;
    }

    /**
     * Create a decompiler with the default atom table.
     */
    public static FdoDecompiler create() {
        return new FdoDecompiler(AtomTable.loadDefault(), false);
    }

    /**
     * Create a decompiler with a custom atom table.
     */
    public static FdoDecompiler create(AtomTable atomTable) {
        return new FdoDecompiler(atomTable, false);
    }

    /**
     * Create a decompiler with the default atom table and unknown format preservation.
     *
     * @param preserveUnknownFormat If true, output {@code the_unknown} format even for known atoms
     */
    public static FdoDecompiler create(boolean preserveUnknownFormat) {
        return new FdoDecompiler(AtomTable.loadDefault(), preserveUnknownFormat);
    }

    /**
     * Create a decompiler with a custom atom table and unknown format preservation.
     *
     * @param atomTable The atom table to use
     * @param preserveUnknownFormat If true, output {@code the_unknown} format even for known atoms
     */
    public static FdoDecompiler create(AtomTable atomTable, boolean preserveUnknownFormat) {
        return new FdoDecompiler(atomTable, preserveUnknownFormat);
    }

    /**
     * Decompile binary FDO data to source text.
     *
     * @param binary The binary FDO data
     * @return The decompiled source text
     * @throws FdoException if decompilation fails
     */
    public String decompile(byte[] binary) throws FdoException {
        // Decode binary to atom frames
        BinaryDecoder decoder = new BinaryDecoder(binary);
        List<AtomFrame> frames = decoder.decode();

        // Convert frames to AST
        StreamNode stream = framesToStream(frames);

        // Format AST to source text
        return formatter.format(stream);
    }

    /**
     * Decompile binary FDO data from an input stream.
     *
     * @param input The input stream containing binary FDO data
     * @return The decompiled source text
     * @throws FdoException if decompilation fails
     * @throws IOException if reading fails
     */
    public String decompile(InputStream input) throws FdoException, IOException {
        return decompile(input.readAllBytes());
    }

    /**
     * Decompile binary FDO data from a file.
     *
     * @param file The file containing binary FDO data
     * @return The decompiled source text
     * @throws FdoException if decompilation fails
     * @throws IOException if reading fails
     */
    public String decompile(File file) throws FdoException, IOException {
        try (InputStream input = new FileInputStream(file)) {
            return decompile(input);
        }
    }

    /**
     * Convert atom frames to an AST stream.
     */
    private StreamNode framesToStream(List<AtomFrame> frames) throws FdoException {
        List<AtomNode> atoms = new ArrayList<>();

        for (AtomFrame frame : frames) {
            atoms.add(frameToAtom(frame));
        }

        return new StreamNode(atoms, 1, 1);
    }

    /**
     * Convert an atom frame to an AST node.
     */
    private AtomNode frameToAtom(AtomFrame frame) throws FdoException {
        int protocol = frame.protocol();
        int atomNumber = frame.atomNumber();
        byte[] data = frame.data();

        // Look up atom definition
        Optional<AtomDefinition> defOpt = atomTable.findByProtocolAtom(protocol, atomNumber);
        AtomDefinition definition = defOpt.orElse(null);

        String name;
        List<ArgumentNode> arguments;

        if (definition != null && !preserveUnknownFormat) {
            // Known atom - use canonical name
            name = definition.canonicalName();
            arguments = decodeDataToArguments(data, definition);
        } else {
            // Unknown atom OR preserve unknown format - output as the_unknown format
            name = "the_unknown";
            arguments = decodeUnknownAtomArguments(protocol, atomNumber, data);
        }

        return new AtomNode(name, arguments, definition, 1, 1);
    }

    /**
     * Decode binary data to argument nodes based on atom type.
     */
    private List<ArgumentNode> decodeDataToArguments(byte[] data, AtomDefinition def) throws FdoException {
        if (data == null || data.length == 0) {
            return List.of();
        }

        // First check for atom-specific handling (overrides type-based decoding)
        if (def != null) {
            ArgumentNode specialArg = decodeSpecialAtom(data, def);
            if (specialArg != null) {
                return List.of(specialArg);
            }
        }

        AtomType type = def != null ? def.type() : AtomType.RAW;

        ArgumentNode arg = switch (type) {
            case RAW -> decodeRawData(data);
            case DWORD -> decodeDwordData(data);
            case STRING -> decodeStringData(data);
            case GID -> decodeGidData(data);
            case OBJSTART -> decodeObjectTypeData(data);
            case STREAM -> decodeStreamData(data);
            case ATOM -> decodeAtomRef(data);
            case BOOL -> decodeBoolData(data);
            case TOKEN -> decodeRawData(data);
            case ORIENT -> decodeOrientData(data);
            case CRITERION -> decodeCriterionData(data);
            default -> decodeRawData(data);
        };

        return arg != null ? List.of(arg) : List.of();
    }

    /**
     * Handle atoms that need special decoding regardless of their declared type.
     * Returns null if no special handling needed.
     */
    private ArgumentNode decodeSpecialAtom(byte[] data, AtomDefinition def) throws FdoException {
        int proto = def.protocol();
        int atom = def.atomNumber();

        // MAT protocol (16) special atoms
        if (proto == 16) {
            return switch (atom) {
                case 10 -> decodeFontSisData(data);        // mat_font_sis
                case 64 -> decodePositionData(data);       // mat_position
                case 65 -> decodeLogObjectData(data);      // mat_log_object
                case 69 -> decodeSortOrderData(data);      // mat_sort_order
                case 87 -> decodeFrameStyleData(data);     // mat_frame_style
                case 88 -> decodeTriggerStyleData(data);   // mat_trigger_style
                case 17 -> decodeTitlePosData(data);       // mat_title_pos
                case 18 -> decodeDecimalNumber(data);      // mat_ruler
                case 20 -> decodeDecimalNumber(data);      // mat_vertical_spacing
                case 23 -> decodeSizeData(data);           // mat_size
                case 48 -> decodeFontIdData(data);         // mat_font_id
                case 49 -> decodeDecimalNumber(data);      // mat_font_size
                case 50 -> decodeFontStyleData(data);      // mat_font_style
                case 53 -> decodeHexListData(data);        // mat_command_key (e.g., 91x, 00x)
                case 56 -> decodeDecimalNumber(data);      // mat_validation
                case 57 -> decodeDecimalNumber(data);      // mat_horizontal_spacing
                case 11 -> decodeDecimalNumber(data);      // mat_relative_tag
                case 58 -> decodeStyleIdData(data);        // mat_style_id
                case 14 -> decodeArtIdData(data);          // mat_art_id
                default -> null;
            };
        }

        // ACT protocol (2) special atoms
        if (proto == 2) {
            return switch (atom) {
                case 0 -> decodeCriterionValue(data);      // act_set_criterion
                case 1 -> decodeCriterionValue(data);      // act_do_action
                case 53 -> decodeHexByteListData(data);    // act_get_db_value - comma-separated hex bytes
                default -> null;
            };
        }

        // ASYNC protocol (13) special atoms
        if (proto == 13) {
            return switch (atom) {
                case 14 -> decodeHexByteListData(data);    // async_exec_context_help - comma-separated hex bytes
                default -> null;
            };
        }

        // DE protocol (3) special atoms
        if (proto == 3) {
            return switch (atom) {
                case 2 -> decodeDecimalNumber(data);       // de_start_extraction - decimal
                case 10 -> decodeStringData(data);         // de_ez_send_form - string arg
                default -> null;
            };
        }

        // SM protocol (14) special atoms
        if (proto == 14) {
            return switch (atom) {
                case 3 -> decodeSmSendGid(data);           // sm_send_k1 - GID (3 bytes)
                case 12 -> decodeSmTokenArg(data);         // sm_send_token_arg - string or string+number
                default -> null;
            };
        }

        // IF protocol (15) - decode conditional atoms as comma-separated lists
        if (proto == 15) {
            return decodeByteListData(data);
        }

        // DOD protocol (27) special atoms - GID atoms that use 3-byte encoding for type=0, subtype>0
        if (proto == 27) {
            return switch (atom) {
                case 2, 3 -> decodeDodGid(data);              // dod_gid, dod_form_id - may be 3-byte with type=0
                default -> null;
            };
        }

        // UNI protocol (0) special atoms
        if (proto == 0) {
            return switch (atom) {
                case 10, 11 -> decodeAtomRef(data);          // uni_use_last_atom_string/value
                default -> null;
            };
        }

        // MAN protocol (1) special atoms
        if (proto == 1) {
            return switch (atom) {
                case 9 -> decodeGidOrDecimal(data);          // man_set_context_globalid
                case 10 -> decodeDecimalNumber(data);        // man_set_context_relative
                default -> null;
            };
        }

        // IDB protocol (5) special atoms
        if (proto == 5) {
            return switch (atom) {
                case 19 -> decodeFullGid(data);              // idb_set_context - always 3-part
                default -> null;
            };
        }

        // VAR protocol (12) special atoms
        if (proto == 12) {
            return switch (atom) {
                case 0 -> decodeVarLookupArg(data);          // var_number_save (letter, big number)
                case 1 -> decodeLetterNumberArg(data);       // var_number_set (letter, number)
                case 2, 48 -> decodeLetterArg(data);         // var_number_set_from_atom, var_number_decrement
                case 46 -> decodeLetterArg(data);            // var_number_clear_id
                case 5 -> decodeLetterStringArg(data);       // var_string_set (letter, string)
                case 6 -> decodeLetterArg(data);             // var_string_null
                case 8, 9 -> decodeLetterArg(data);          // var_string_set_from_atom, var_string_get
                case 19 -> decodeVarLookupArg(data);         // var_lookup_by_id (letter, big number)
                case 69 -> decodeLetterArg(data);            // var_string_clear_id
                default -> null;
            };
        }

        // LM protocol (9) special atoms
        if (proto == 9) {
            return switch (atom) {
                case 17 -> decodeFullGid(data);              // lm_table_use_table - always 3-part GID
                default -> null;
            };
        }

        return null;
    }

    /**
     * Decode criterion value - named constants or decimal numbers.
     * Note: Only select/gain_focus are output as names, others as numbers.
     */
    private ArgumentNode decodeCriterionValue(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        int code = data[0] & 0xFF;
        // Only these criterion values are named
        String name = switch (code) {
            case 1 -> "select";
            case 4 -> "gain_focus";
            default -> null;
        };
        if (name != null) {
            return new ArgumentNode.IdentifierArg(name, 1, 1);
        }
        return new ArgumentNode.NumberArg(code, 1, 1);
    }

    /**
     * Decode as letter + number pair (e.g., A, 1)
     */
    private ArgumentNode decodeLetterNumberArg(byte[] data) {
        if (data.length < 2) {
            return decodeRawData(data);
        }
        char letter = (char) ('A' + (data[0] & 0xFF));
        int number = 0;
        for (int i = 1; i < data.length; i++) {
            number = (number << 8) | (data[i] & 0xFF);
        }
        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.IdentifierArg(String.valueOf(letter), 1, 1));
        parts.add(new ArgumentNode.NumberArg(number, 1, 1));
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode as GID if 3+ bytes, otherwise as decimal number.
     */
    private ArgumentNode decodeGidOrDecimal(byte[] data) {
        if (data.length >= 3) {
            return decodeGidData(data);
        }
        return decodeDecimalNumber(data);
    }

    /**
     * Decode as full 3-part GID (type-subtype-id), always showing subtype.
     */
    private ArgumentNode decodeFullGid(byte[] data) {
        if (data.length < 4) {
            return decodeRawData(data);
        }

        int type = data[0] & 0xFF;
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

        String gidStr = type + "-" + subtype + "-" + id;
        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    /**
     * Decode single byte as letter (0=A, 1=B, etc.)
     */
    private ArgumentNode decodeLetterArg(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        char letter = (char) ('A' + (data[0] & 0xFF));
        return new ArgumentNode.IdentifierArg(String.valueOf(letter), 1, 1);
    }

    /**
     * Decode as letter + string pair (e.g., B,"New UseR  ")
     * Format: [letter byte] [string bytes] [null]
     */
    private ArgumentNode decodeLetterStringArg(byte[] data) {
        if (data.length < 2) {
            return decodeRawData(data);
        }
        char letter = (char) ('A' + (data[0] & 0xFF));

        // Find null terminator for string
        int len = 1;
        while (len < data.length && data[len] != 0) {
            len++;
        }
        String str = new String(data, 1, len - 1, StandardCharsets.UTF_8);

        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.IdentifierArg(String.valueOf(letter), 1, 1));
        parts.add(new ArgumentNode.StringArg(str, 1, 1));
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode var_lookup_by_id format: letter + big decimal number (e.g., A, 2147483647)
     * Format: [letter byte] [4 bytes big-endian number]
     */
    private ArgumentNode decodeVarLookupArg(byte[] data) {
        if (data.length < 2) {
            return decodeRawData(data);
        }
        char letter = (char) ('A' + (data[0] & 0xFF));

        // Decode big-endian number from remaining bytes
        long number = 0;
        for (int i = 1; i < data.length; i++) {
            number = (number << 8) | (data[i] & 0xFF);
        }

        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.IdentifierArg(String.valueOf(letter), 1, 1));
        parts.add(new ArgumentNode.NumberArg(number, 1, 1));
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode font ID as font name.
     */
    private ArgumentNode decodeFontIdData(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        int code = data[0] & 0xFF;
        String fontName = switch (code) {
            case 0 -> "arial";
            case 1 -> "courier";
            case 2 -> "times_roman";
            case 3 -> "system";
            case 4 -> "fixed_system";
            case 5 -> "ms_serif";
            case 6 -> "ms_sans_serif";
            case 7 -> "small_fonts";
            case 8 -> "courier_new";
            case 9 -> "script";
            case 10 -> "ms_mincho";
            case 11 -> "ms_gothic";
            default -> String.format("%02xx", code);
        };
        return new ArgumentNode.IdentifierArg(fontName, 1, 1);
    }

    /**
     * Decode data as comma-separated list of byte values.
     */
    private ArgumentNode decodeByteListData(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        if (data.length == 1) {
            return new ArgumentNode.NumberArg(data[0] & 0xFF, 1, 1);
        }

        List<ArgumentNode> parts = new ArrayList<>();
        for (byte b : data) {
            parts.add(new ArgumentNode.NumberArg(b & 0xFF, 1, 1));
        }
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode data as comma-separated list of hex values (e.g., "91x, 00x").
     * Returns single HexArg or ListArg of HexArgs.
     */
    private ArgumentNode decodeHexListData(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        if (data.length == 1) {
            return new ArgumentNode.HexArg(String.format("%02xx", data[0] & 0xFF), 1, 1);
        }

        List<ArgumentNode> parts = new ArrayList<>();
        for (byte b : data) {
            parts.add(new ArgumentNode.HexArg(String.format("%02xx", b & 0xFF), 1, 1));
        }
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode data as comma-separated list of individual hex bytes (e.g., "14x, 00x, 00x, 2ex").
     * Always formats each byte individually with format "XXx".
     */
    private ArgumentNode decodeHexByteListData(byte[] data) {
        if (data.length == 0) {
            return null;
        }
        if (data.length == 1) {
            return new ArgumentNode.HexArg(String.format("%02xx", data[0] & 0xFF), 1, 1);
        }

        List<ArgumentNode> parts = new ArrayList<>();
        for (byte b : data) {
            parts.add(new ArgumentNode.HexArg(String.format("%02xx", b & 0xFF), 1, 1));
        }
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode token argument as "string", number format.
     * Format: [string bytes] [number byte]
     */
    private ArgumentNode decodeTokenArgData(byte[] data) {
        if (data.length < 2) {
            return decodeRawData(data);
        }

        // Last byte is the number, rest is the string
        int strLen = data.length - 1;
        String str = new String(data, 0, strLen, StandardCharsets.UTF_8);
        int num = data[strLen] & 0xFF;

        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.StringArg(str, 1, 1));
        parts.add(new ArgumentNode.NumberArg(num, 1, 1));
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode mat_position as named position constant.
     */
    private ArgumentNode decodePositionData(byte[] data) {
        if (data.length == 0) return null;
        int code = data[0] & 0xFF;
        String name = switch (code) {
            case 0 -> "cascade";
            case 1 -> "top_left";
            case 2 -> "top_center";
            case 3 -> "top_right";
            case 4 -> "center_left";
            case 5 -> "center_center";
            case 6 -> "center_right";
            case 7 -> "bottom_left";
            case 8 -> "bottom_center";
            case 9 -> "bottom_right";
            default -> String.format("%02xx", code);
        };
        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    /**
     * Decode mat_log_object as named log object type.
     * Values: session_log=0, chat_log=1, im_log=2, no_log=3
     */
    private ArgumentNode decodeLogObjectData(byte[] data) {
        if (data.length == 0) return null;
        int code = data[0] & 0xFF;
        String name = switch (code) {
            case 0 -> "session_log";
            case 1 -> "chat_log";
            case 2 -> "im_log";
            case 3 -> "no_log";
            default -> String.format("%02xx", code);
        };
        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    /**
     * Decode mat_sort_order as named sort order.
     * Values: normal=0, reverse=1, alphabetical=2
     */
    private ArgumentNode decodeSortOrderData(byte[] data) {
        if (data.length == 0) return null;
        int code = data[0] & 0xFF;
        String name = switch (code) {
            case 0 -> "normal";
            case 1 -> "reverse";
            case 2 -> "alphabetical";
            default -> String.format("%02xx", code);
        };
        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    /**
     * Decode mat_frame_style as named frame style (2-byte big-endian).
     * Values: none=0, single_line_pop_out=1, single_line_pop_in=2, pop_in=3,
     *         pop_out=4, double_line=5, shadow=6, highlight=7
     */
    private ArgumentNode decodeFrameStyleData(byte[] data) {
        if (data.length < 2) return null;
        int code = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        String name = switch (code) {
            case 0 -> "none";
            case 1 -> "single_line_pop_out";
            case 2 -> "single_line_pop_in";
            case 3 -> "pop_in";
            case 4 -> "pop_out";
            case 5 -> "double_line";
            case 6 -> "shadow";
            case 7 -> "highlight";
            default -> String.format("%d", code);
        };
        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    /**
     * Decode mat_trigger_style as named trigger style (2-byte big-endian).
     * Values: default=0, place=1, rectangle=2, picture=3, framed=4,
     *         bottom_tab=5, plain_picture=6, group_state=7
     */
    private ArgumentNode decodeTriggerStyleData(byte[] data) {
        if (data.length < 2) return null;
        int code = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        String name = switch (code) {
            case 0 -> "default";
            case 1 -> "place";
            case 2 -> "rectangle";
            case 3 -> "picture";
            case 4 -> "framed";
            case 5 -> "bottom_tab";
            case 6 -> "plain_picture";
            case 7 -> "group_state";
            default -> String.format("%d", code);
        };
        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    /**
     * Decode mat_title_pos as piped orientation (e.g., "left | center").
     */
    private ArgumentNode decodeTitlePosData(byte[] data) {
        if (data.length == 0) return null;
        int code = data[0] & 0xFF;

        // Title position is: orientation (bits 7-6) + justify (bits 1-0)
        int orient = (code >> 6) & 0x03;
        int justify = code & 0x03;

        String orientStr = switch (orient) {
            case 0 -> "left";
            case 1 -> "above";
            case 2 -> "right";
            case 3 -> "below";
            default -> "left";
        };

        String justifyStr = switch (justify) {
            case 0 -> "center";
            case 1 -> "top_or_left";
            case 2 -> "bottom_or_right";
            default -> "center";
        };

        // Return as piped argument
        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.IdentifierArg(orientStr, 1, 1));
        parts.add(new ArgumentNode.IdentifierArg(justifyStr, 1, 1));
        return new ArgumentNode.PipedArg(parts, 1, 1);
    }

    /**
     * Decode mat_size as comma-separated values (e.g., "25, 1, 32").
     */
    private ArgumentNode decodeSizeData(byte[] data) {
        if (data.length == 0) return null;

        List<ArgumentNode> parts = new ArrayList<>();

        // Size is typically: width (1 byte), height (1 byte), max_chars (2 bytes big-endian)
        if (data.length >= 1) {
            parts.add(new ArgumentNode.NumberArg(data[0] & 0xFF, 1, 1));
        }
        if (data.length >= 2) {
            parts.add(new ArgumentNode.NumberArg(data[1] & 0xFF, 1, 1));
        }
        if (data.length >= 4) {
            int maxChars = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            parts.add(new ArgumentNode.NumberArg(maxChars, 1, 1));
        } else if (data.length >= 3) {
            parts.add(new ArgumentNode.NumberArg(data[2] & 0xFF, 1, 1));
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode single-byte or multi-byte value as decimal number.
     */
    private ArgumentNode decodeDecimalNumber(byte[] data) {
        if (data.length == 0) return null;

        int value;
        if (data.length == 1) {
            value = data[0] & 0xFF;
        } else if (data.length == 2) {
            value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        } else {
            // Larger values - use big-endian
            value = 0;
            for (byte b : data) {
                value = (value << 8) | (b & 0xFF);
            }
        }
        return new ArgumentNode.NumberArg(value, 1, 1);
    }

    /**
     * Decode mat_style_id as 2-part GID format (e.g., "32-100").
     * Style IDs use type + 2-byte id format (no subtype).
     */
    private ArgumentNode decodeStyleIdData(byte[] data) {
        if (data.length < 3) {
            return decodeRawData(data);
        }

        int type = data[0] & 0xFF;
        int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);

        String gidStr = type + "-" + id;
        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    /**
     * Decode mat_art_id as 3-part GID format (e.g., "1-0-1320").
     * Art IDs always use type-subtype-id format.
     */
    private ArgumentNode decodeArtIdData(byte[] data) {
        if (data.length < 4) {
            return decodeRawData(data);
        }

        int type = data[0] & 0xFF;
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

        String gidStr = type + "-" + subtype + "-" + id;
        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    /**
     * Decode sm_send_token_arg - string, or string+number if last byte is non-printable.
     * The last byte is output as a separate number if it's < 32 or >= 127.
     */
    private ArgumentNode decodeSmTokenArg(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        // Check if last byte is non-printable
        int lastByte = data[data.length - 1] & 0xFF;
        boolean lastIsPrintable = lastByte >= 32 && lastByte < 127;

        if (lastIsPrintable || data.length == 1) {
            // Just output as string
            return decodeStringData(data);
        } else {
            // Split: string + number
            String str = new String(data, 0, data.length - 1, StandardCharsets.UTF_8);
            List<ArgumentNode> parts = new ArrayList<>();
            parts.add(new ArgumentNode.StringArg(str, 1, 1));
            parts.add(new ArgumentNode.NumberArg(lastByte, 1, 1));
            return new ArgumentNode.ListArg(parts, 1, 1);
        }
    }

    /**
     * Decode sm_send GID (3-byte format: type + 2-byte id, e.g., "8-50934").
     */
    private ArgumentNode decodeSmSendGid(byte[] data) {
        if (data.length < 3) {
            return decodeRawData(data);
        }

        int type = data[0] & 0xFF;
        int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);

        String gidStr = type + "-" + id;
        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    /**
     * Decode mat_font_style as named style (bold, italic, underline, etc.)
     */
    private ArgumentNode decodeFontStyleData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        int code = data[0] & 0xFF;
        String style = switch (code) {
            case 0 -> "normal";
            case 1 -> "bold";
            case 2 -> "italic";
            case 3 -> "bold_italic";
            case 4 -> "underline";
            default -> String.format("%02xx", code);
        };
        return new ArgumentNode.IdentifierArg(style, 1, 1);
    }

    /**
     * Decode mat_font_sis as comma-separated font name, size, and style.
     * Format: [font_id byte] [size byte] [style byte]
     * Output: <font_name, size, style>
     */
    private ArgumentNode decodeFontSisData(byte[] data) {
        if (data.length < 3) {
            return decodeRawData(data);
        }

        int fontCode = data[0] & 0xFF;
        int fontSize = data[1] & 0xFF;
        int styleCode = data[2] & 0xFF;

        // Font name from code (same mapping as decodeFontIdData)
        String fontName = switch (fontCode) {
            case 0 -> "arial";
            case 1 -> "courier";
            case 2 -> "times_roman";
            case 3 -> "system";
            case 4 -> "fixed_system";
            case 5 -> "ms_serif";
            case 6 -> "ms_sans_serif";
            case 7 -> "small_fonts";
            case 8 -> "courier_new";
            case 9 -> "script";
            case 10 -> "ms_mincho";
            case 11 -> "ms_gothic";
            default -> String.format("%02xx", fontCode);
        };

        // Style name from code (same mapping as decodeFontStyleData)
        String styleName = switch (styleCode) {
            case 0 -> "normal";
            case 1 -> "bold";
            case 2 -> "italic";
            case 3 -> "bold_italic";
            case 4 -> "underline";
            default -> String.format("%02xx", styleCode);
        };

        List<ArgumentNode> parts = new ArrayList<>();
        parts.add(new ArgumentNode.IdentifierArg(fontName, 1, 1));
        parts.add(new ArgumentNode.NumberArg(fontSize, 1, 1));
        parts.add(new ArgumentNode.IdentifierArg(styleName, 1, 1));
        return new ArgumentNode.ListArg(parts, 1, 1);
    }

    /**
     * Decode unknown atom data into the_unknown format arguments.
     * Format: the_unknown <protocol, atom_number, data...>
     *
     * @param protocol The protocol number
     * @param atomNumber The atom number
     * @param data The binary data payload
     * @return List of argument nodes in the_unknown format
     */
    private List<ArgumentNode> decodeUnknownAtomArguments(int protocol, int atomNumber, byte[] data) {
        List<ArgumentNode> args = new ArrayList<>();

        // First argument: protocol number
        args.add(new ArgumentNode.NumberArg(protocol, 1, 1));

        // Second argument: atom number
        args.add(new ArgumentNode.NumberArg(atomNumber, 1, 1));

        // Remaining arguments: convert binary data to hex arguments
        if (data != null && data.length > 0) {
            // Convert binary to hex arguments (comma-separated for multiple bytes)
            List<ArgumentNode> dataArgs = new ArrayList<>();
            for (byte b : data) {
                dataArgs.add(new ArgumentNode.HexArg(
                    String.format("%02xx", b & 0xFF), 1, 1));
            }

            // If single byte, add directly; if multiple, wrap in ListArg
            if (dataArgs.size() == 1) {
                args.addAll(dataArgs);
            } else {
                args.add(new ArgumentNode.ListArg(dataArgs, 1, 1));
            }
        }

        return args;
    }

    private ArgumentNode decodeRawData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        hex.append("x");

        return new ArgumentNode.HexArg(hex.toString(), 1, 1);
    }

    private ArgumentNode decodeDwordData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        // DWORD is stored big-endian with leading zeros trimmed
        // So it can be 1, 2, 3, or 4 bytes
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
        }

        return new ArgumentNode.NumberArg(value, 1, 1);
    }

    private ArgumentNode decodeStringData(byte[] data) {
        // Find null terminator
        int len = 0;
        while (len < data.length && data[len] != 0) {
            len++;
        }

        // Build string with escape sequences for non-printable characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int ch = data[i] & 0xFF;
            if (ch == '\\') {
                sb.append("\\\\");
            } else if (ch == '"') {
                sb.append("\\\"");
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\t') {
                sb.append("\\t");
            } else if (ch >= 0x20 && ch < 0x7F) {
                // Printable ASCII
                sb.append((char) ch);
            } else {
                // Non-printable: use \xHH format
                sb.append(String.format("\\x%02x", ch));
            }
        }

        return new ArgumentNode.StringArg(sb.toString(), 1, 1);
    }

    private ArgumentNode decodeGidData(byte[] data) {
        if (data.length < 3) {
            return decodeRawData(data);
        }

        int type = data[0] & 0xFF;

        if (data.length == 3) {
            // 3-byte GID: type + 2-byte id (no subtype)
            int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            return new ArgumentNode.GidArg(type + "-" + id, 1, 1);
        }

        // 4-byte GID: type + subtype + 2-byte id
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

        String gidStr;
        if (subtype == 0) {
            gidStr = type + "-" + id;
        } else {
            gidStr = type + "-" + subtype + "-" + id;
        }

        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    /**
     * Decode DOD protocol GID atoms (dod_gid, dod_form_id).
     * These atoms can have 3-byte GIDs with type=0, subtype>0 format: [subtype, id_high, id_low]
     */
    private ArgumentNode decodeDodGid(byte[] data) {
        if (data.length < 3) {
            return decodeRawData(data);
        }

        if (data.length == 3) {
            // 3-byte: could be 2-part (type-id) or 3-part with type=0 (subtype-id)
            // For DOD atoms, assume 3-part with type=0: [subtype, id_high, id_low]
            int subtype = data[0] & 0xFF;
            int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            return new ArgumentNode.GidArg("0-" + subtype + "-" + id, 1, 1);
        }

        // 4-byte: type + subtype + 2-byte id (full format)
        int type = data[0] & 0xFF;
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

        String gidStr;
        if (subtype == 0) {
            gidStr = type + "-" + id;
        } else {
            gidStr = type + "-" + subtype + "-" + id;
        }

        return new ArgumentNode.GidArg(gidStr, 1, 1);
    }

    private ArgumentNode decodeObjectTypeData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        int typeCode = data[0] & 0xFF;
        String typeName = codeToObjectType(typeCode);

        // Extract title (may be empty string if only null terminator follows)
        String title = "";
        if (data.length > 1) {
            int len = 1;
            while (len < data.length && data[len] != 0) {
                len++;
            }
            title = new String(data, 1, len - 1, StandardCharsets.UTF_8);
        }

        // Always return ObjectTypeArg with title (even if empty)
        return new ArgumentNode.ObjectTypeArg(typeName, title, 1, 1);
    }

    private ArgumentNode decodeStreamData(byte[] data) throws FdoException {
        if (data.length == 0) {
            return null;
        }

        // First try standard BinaryDecoder (for FULL-style encoded streams)
        try {
            BinaryDecoder nestedDecoder = new BinaryDecoder(data);
            List<AtomFrame> frames = nestedDecoder.decode();
            StreamNode stream = framesToStream(frames);
            return new ArgumentNode.NestedStreamArg(stream, 1, 1);
        } catch (FdoException e) {
            // Failed to decode as FULL-style stream, try raw single-atom format
        }

        // Try raw single-atom format: [proto][atom][data...]
        // This is used when a nested stream contains exactly one atom
        if (data.length >= 2) {
            int protocol = data[0] & 0xFF;
            int atomNum = data[1] & 0xFF;

            Optional<AtomDefinition> def = atomTable.findByProtocolAtom(protocol, atomNum);
            if (def.isPresent()) {
                // Extract the remaining bytes as the atom's data
                byte[] atomData = new byte[data.length - 2];
                System.arraycopy(data, 2, atomData, 0, atomData.length);

                // Create a single-frame stream with this atom
                AtomFrame frame = new AtomFrame(protocol, atomNum, atomData);
                List<AtomFrame> frames = List.of(frame);
                StreamNode stream = framesToStream(frames);
                return new ArgumentNode.NestedStreamArg(stream, 1, 1);
            }
        }

        // Couldn't decode as stream, return as raw data
        throw new FdoException(FdoException.ErrorCode.INVALID_BINARY_FORMAT,
            "Cannot decode nested stream data: " + data.length + " bytes");
    }

    private ArgumentNode decodeAtomRef(byte[] data) {
        if (data.length < 2) {
            return decodeRawData(data);
        }

        int protocol = data[0] & 0xFF;
        int atomNum = data[1] & 0xFF;

        Optional<AtomDefinition> def = atomTable.findByProtocolAtom(protocol, atomNum);
        String name = def.map(AtomDefinition::canonicalName)
                        .orElse(String.format("proto%d_atom%d", protocol, atomNum));

        return new ArgumentNode.IdentifierArg(name, 1, 1);
    }

    private ArgumentNode decodeBoolData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        String value = (data[0] != 0) ? "yes" : "no";
        return new ArgumentNode.IdentifierArg(value, 1, 1);
    }

    private ArgumentNode decodeOrientData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        // Decode orientation compound code
        // Bit layout: 0 VH HHHJJJ
        //   Bit 6: 0=horizontal, 1=vertical
        //   Bits 5-3: horizontal justify (c=0, l=1, r=2, f=3, e=4)
        //   Bits 2-0: vertical justify (t=0, c=1, b=2, f=3, e=4)
        int flags = data[0] & 0xFF;

        StringBuilder orient = new StringBuilder();

        // Vertical/horizontal (bit 6)
        if ((flags & 0x40) != 0) {
            orient.append('v');
        } else {
            orient.append('h');
        }

        // Horizontal justify (bits 5-3)
        int hj = (flags >> 3) & 0x07;
        orient.append(switch (hj) {
            case 0 -> 'c';  // center
            case 1 -> 'l';  // left
            case 2 -> 'r';  // right
            case 3 -> 'f';  // full
            case 4 -> 'e';  // even
            default -> 'c';
        });

        // Vertical justify (bits 2-0)
        int vj = flags & 0x07;
        orient.append(switch (vj) {
            case 0 -> 'c';  // center
            case 1 -> 't';  // top
            case 2 -> 'b';  // bottom
            case 3 -> 'f';  // full
            case 4 -> 'e';  // even
            default -> 'c';
        });

        return new ArgumentNode.IdentifierArg(orient.toString(), 1, 1);
    }

    private ArgumentNode decodeCriterionData(byte[] data) {
        if (data.length == 0) {
            return null;
        }

        // Criterion is usually a single byte value
        int value = data[0] & 0xFF;

        // Format as hex for criterion
        return new ArgumentNode.HexArg(String.format("%02xx", value), 1, 1);
    }

    private String codeToObjectType(int code) {
        // FDO text names from golden test files - these are the canonical short forms
        return switch (code) {
            case 0 -> "org_group";
            case 1 -> "ind_group";
            case 2 -> "dms_list";
            case 3 -> "sms_list";
            case 4 -> "dss_list";
            case 5 -> "sss_list";
            case 6 -> "trigger";
            case 7 -> "ornament";
            case 8 -> "view";
            case 9 -> "edit_view";
            case 10 -> "boolean";
            case 11 -> "select_boolean";
            case 12 -> "range";
            case 13 -> "select_range";
            case 14 -> "variable";
            case 16 -> "root";
            default -> "unknown_" + code;
        };
    }
}
