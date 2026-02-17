package com.atomforge.fdo.model;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.BinaryDecoder;
import com.atomforge.fdo.dsl.values.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decodes AtomFrame binary data into FdoValue objects.
 * Ported from FdoDecompiler with focus on producing native Java types.
 */
public final class ValueDecoder {

    private ValueDecoder() {} // utility class

    /**
     * Decode an AtomFrame into an FdoAtom.
     *
     * @param frame The binary frame
     * @param atomTable The atom lookup table
     * @return Decoded FdoAtom with rawData preserved for round-trip fidelity
     */
    public static FdoAtom decode(AtomFrame frame, AtomTable atomTable) throws FdoException {
        int protocol = frame.protocol();
        int atomNumber = frame.atomNumber();
        byte[] data = frame.data();

        // Look up atom definition
        Optional<AtomDefinition> defOpt = atomTable.findByProtocolAtom(protocol, atomNumber);
        AtomDefinition definition = defOpt.orElse(null);

        String name;
        AtomType type;
        if (definition != null) {
            name = definition.canonicalName();
            type = definition.type();
        } else {
            // Unknown atom - use proto_atom format
            name = String.format("proto%d_atom%d", protocol, atomNumber);
            type = AtomType.RAW;
        }

        // Decode data to value
        FdoValue value = decodeValue(data, definition, atomTable);

        // Preserve raw data for round-trip fidelity
        return new FdoAtom(name, protocol, atomNumber, type, value, data);
    }

    /**
     * Decode binary data to FdoValue based on atom definition.
     */
    private static FdoValue decodeValue(byte[] data, AtomDefinition def, AtomTable atomTable) throws FdoException {
        if (data == null || data.length == 0) {
            return FdoValue.EmptyValue.instance();
        }

        // First check for atom-specific handling (overrides type-based decoding)
        if (def != null) {
            FdoValue specialValue = decodeSpecialAtom(data, def, atomTable);
            if (specialValue != null) {
                return specialValue;
            }
        }

        AtomType type = def != null ? def.type() : AtomType.RAW;

        return switch (type) {
            case RAW -> decodeRaw(data);
            case DWORD -> decodeNumber(data);
            case STRING -> decodeString(data);
            case GID -> decodeGid(data);
            case OBJSTART -> decodeObjectType(data);
            case STREAM -> decodeStream(data, atomTable);
            case ATOM -> decodeAtomRef(data, atomTable);
            case BOOL -> decodeBoolean(data);
            case ORIENT -> decodeOrientation(data);
            case CRITERION -> decodeNumber(data); // criterion as number
            default -> decodeRaw(data);
        };
    }

    /**
     * Handle atoms that need special decoding regardless of their declared type.
     * Returns null if no special handling needed.
     */
    private static FdoValue decodeSpecialAtom(byte[] data, AtomDefinition def, AtomTable atomTable) throws FdoException {
        int proto = def.protocol();
        int atom = def.atomNumber();

        // MAT protocol (16) special atoms
        if (proto == 16) {
            return switch (atom) {
                case 23 -> decodeSizeList(data);           // mat_size - list of numbers
                case 48 -> decodeFontId(data);             // mat_font_id - identifier
                case 49 -> decodeNumber(data);             // mat_font_size - number
                case 50 -> decodeFontStyle(data);          // mat_font_style - identifier
                case 64 -> decodePosition(data);           // mat_position - identifier
                case 17 -> decodeTitlePos(data);           // mat_title_pos - orientation-like
                case 18, 20, 56, 57, 11 -> decodeNumber(data); // decimal numbers
                case 53 -> decodeRaw(data);                // mat_command_key - raw
                case 58 -> decodeStyleId(data);            // mat_style_id - GID
                case 14 -> decodeArtId(data);              // mat_art_id - 3-part GID
                default -> null;
            };
        }

        // ACT protocol (2) special atoms
        if (proto == 2) {
            return switch (atom) {
                case 0, 1 -> decodeCriterion(data);        // act_set_criterion, act_do_action
                case 53 -> decodeRaw(data);                // act_get_db_value - raw bytes
                default -> null;
            };
        }

        // DE protocol (3) special atoms - string data (like de_data)
        if (proto == 3) {
            return switch (atom) {
                case 1 -> decodeString(data);              // de_data - always string
                case 2 -> decodeNumber(data);              // de_start_extraction - decimal
                case 10 -> decodeString(data);             // de_ez_send_form - string
                default -> null;
            };
        }

        // MAN protocol (1) special atoms
        if (proto == 1) {
            return switch (atom) {
                case 9 -> decodeGidOrNumber(data);         // man_set_context_globalid
                case 10, 11 -> decodeNumber(data);         // man_set_context_relative/index
                default -> null;
            };
        }

        // VAR protocol (12) special atoms
        if (proto == 12) {
            return switch (atom) {
                case 0, 19 -> decodeLetterNumber(data);    // var_number_save, var_lookup_by_id
                case 1 -> decodeLetterNumber(data);        // var_number_set
                case 2, 6, 8, 9, 46, 48, 69 -> decodeLetter(data); // single letter atoms
                case 5 -> decodeLetterString(data);        // var_string_set
                default -> null;
            };
        }

        // SM protocol (14) special atoms
        if (proto == 14) {
            return switch (atom) {
                case 3 -> decodeSmGid(data);               // sm_send_k1 - 2-part GID
                case 12 -> decodeSmTokenArg(data);         // sm_send_token_arg
                default -> null;
            };
        }

        // IF protocol (15) - byte lists
        if (proto == 15) {
            return decodeNumberList(data);
        }

        // IDB protocol (5)
        if (proto == 5 && atom == 19) {
            return decode3PartGid(data);                   // idb_set_context - always 3-part
        }

        // LM protocol (9)
        if (proto == 9 && atom == 17) {
            return decode3PartGid(data);                   // lm_table_use_table - always 3-part
        }

        // DOD protocol (27) special atoms - GID atoms that use 3-byte encoding for type=0, subtype>0
        if (proto == 27) {
            return switch (atom) {
                case 2, 3 -> decodeDodGid(data);              // dod_gid, dod_form_id - may be 3-byte with type=0
                default -> null;
            };
        }

        // UNI protocol (0)
        if (proto == 0 && (atom == 10 || atom == 11)) {
            return decodeAtomRef(data, AtomTable.loadDefault()); // uni_use_last_atom_string/value
        }

        return null;
    }

    // ========== Type-specific decoders ==========

    private static FdoValue decodeRaw(byte[] data) {
        return new FdoValue.RawValue(data);
    }

    private static FdoValue decodeNumber(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.NumberValue(0);
        }
        long value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
        }
        return new FdoValue.NumberValue(value);
    }

    private static FdoValue decodeString(byte[] data) {
        // FDO strings are NOT null-terminated in binary, length is frame length
        // Use ISO-8859-1 to preserve raw bytes
        String value = new String(data, StandardCharsets.ISO_8859_1);
        return new FdoValue.StringValue(value);
    }

    private static FdoValue decodeGid(byte[] data) {
        if (data.length < 3) {
            return decodeRaw(data);
        }

        int type = data[0] & 0xFF;

        if (data.length == 3) {
            // 2-part GID: type + 2-byte id
            int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            return new FdoValue.GidValue(FdoGid.of(type, id));
        }

        // 4-byte GID: type + subtype + 2-byte id (always 3-part, even when subtype=0)
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        return new FdoValue.GidValue(FdoGid.of(type, subtype, id));
    }

    private static FdoValue decode3PartGid(byte[] data) {
        if (data.length < 4) {
            return decodeRaw(data);
        }
        int type = data[0] & 0xFF;
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        return new FdoValue.GidValue(FdoGid.of(type, subtype, id));
    }

    /**
     * Decode DOD protocol GID atoms (dod_gid, dod_form_id).
     * These atoms can have 3-byte GIDs with type=0, subtype>0 format: [subtype, id_high, id_low]
     */
    private static FdoValue decodeDodGid(byte[] data) {
        if (data.length < 3) {
            return decodeRaw(data);
        }

        if (data.length == 3) {
            // 3-byte: could be 2-part (type-id) or 3-part with type=0 (subtype-id)
            // For DOD atoms, assume 3-part with type=0: [subtype, id_high, id_low]
            int subtype = data[0] & 0xFF;
            int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            return new FdoValue.GidValue(FdoGid.of(0, subtype, id));
        }

        // 4-byte: type + subtype + 2-byte id (full format)
        int type = data[0] & 0xFF;
        int subtype = data[1] & 0xFF;
        int id = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        return new FdoValue.GidValue(FdoGid.of(type, subtype, id));
    }

    private static FdoValue decodeGidOrNumber(byte[] data) {
        if (data.length >= 3) {
            return decodeGid(data);
        }
        return decodeNumber(data);
    }

    private static FdoValue decodeBoolean(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.BooleanValue(false);
        }
        return new FdoValue.BooleanValue(data[0] != 0);
    }

    private static FdoValue decodeOrientation(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.OrientationValue("hcc");
        }

        int flags = data[0] & 0xFF;
        StringBuilder orient = new StringBuilder();

        // Vertical/horizontal (bit 6)
        orient.append((flags & 0x40) != 0 ? 'v' : 'h');

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

        return new FdoValue.OrientationValue(orient.toString());
    }

    private static FdoValue decodeObjectType(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.ObjectTypeValue("unknown", "");
        }

        int typeCode = data[0] & 0xFF;
        String typeName = codeToObjectType(typeCode);

        // Extract title (may be empty)
        String title = "";
        if (data.length > 1) {
            title = new String(data, 1, data.length - 1, StandardCharsets.ISO_8859_1);
        }

        return new FdoValue.ObjectTypeValue(typeName, title);
    }

    private static FdoValue decodeStream(byte[] data, AtomTable atomTable) throws FdoException {
        if (data.length == 0) {
            return new FdoValue.StreamValue(new FdoStream(List.of()));
        }

        List<AtomFrame> frames;

        // First try standard BinaryDecoder (for FULL-style encoded streams)
        try {
            BinaryDecoder nestedDecoder = new BinaryDecoder(data);
            frames = nestedDecoder.decode();
        } catch (FdoException e) {
            // Failed to decode as FULL-style stream, try raw single-atom format
            frames = tryRawSingleAtomFormat(data, atomTable);
        }

        List<FdoAtom> atoms = new ArrayList<>(frames.size());
        for (AtomFrame frame : frames) {
            atoms.add(decode(frame, atomTable));
        }

        return new FdoValue.StreamValue(new FdoStream(atoms));
    }

    /**
     * Try to decode raw single-atom format: [proto][atom][data...]
     * This is used when a nested stream contains exactly one atom.
     */
    private static List<AtomFrame> tryRawSingleAtomFormat(byte[] data, AtomTable atomTable) throws FdoException {
        if (data.length >= 2) {
            int protocol = data[0] & 0xFF;
            int atomNum = data[1] & 0xFF;

            Optional<AtomDefinition> def = atomTable.findByProtocolAtom(protocol, atomNum);
            if (def.isPresent()) {
                // Extract the remaining bytes as the atom's data
                byte[] atomData = new byte[data.length - 2];
                System.arraycopy(data, 2, atomData, 0, atomData.length);

                // Create a single-frame list
                return List.of(new AtomFrame(protocol, atomNum, atomData));
            }
        }

        throw new FdoException(FdoException.ErrorCode.INVALID_BINARY_FORMAT,
            "Cannot decode nested stream data: " + data.length + " bytes");
    }

    private static FdoValue decodeAtomRef(byte[] data, AtomTable atomTable) {
        if (data.length < 2) {
            return decodeRaw(data);
        }

        int protocol = data[0] & 0xFF;
        int atomNum = data[1] & 0xFF;

        Optional<AtomDefinition> def = atomTable.findByProtocolAtom(protocol, atomNum);
        String name = def.map(AtomDefinition::canonicalName)
                .orElse(String.format("proto%d_atom%d", protocol, atomNum));

        // Return as string value (atom reference name)
        return new FdoValue.StringValue(name);
    }

    // ========== Special format decoders ==========

    private static FdoValue decodeCriterion(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.NumberValue(0);
        }
        int code = data[0] & 0xFF;
        // Named criterion values
        String name = switch (code) {
            case 1 -> "select";
            case 4 -> "gain_focus";
            default -> null;
        };
        if (name != null) {
            return new FdoValue.StringValue(name);
        }
        return new FdoValue.NumberValue(code);
    }

    private static FdoValue decodeFontId(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.StringValue(FontId.ARIAL.fdoName());
        }
        int code = data[0] & 0xFF;
        return new FdoValue.StringValue(FontId.nameFromCode(code));
    }

    private static FdoValue decodeFontStyle(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.StringValue(FontStyle.NORMAL.fdoName());
        }
        int code = data[0] & 0xFF;
        return new FdoValue.StringValue(FontStyle.nameFromCode(code));
    }

    private static FdoValue decodePosition(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.StringValue(Position.CASCADE.fdoName());
        }
        int code = data[0] & 0xFF;
        return new FdoValue.StringValue(Position.nameFromCode(code));
    }

    private static FdoValue decodeTitlePos(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.StringValue(TitlePosition.LEFT_CENTER.fdoName());
        }
        int code = data[0] & 0xFF;
        return new FdoValue.StringValue(TitlePosition.nameFromCode(code));
    }

    private static FdoValue decodeSizeList(byte[] data) {
        if (data.length == 0) {
            return FdoValue.EmptyValue.instance();
        }

        List<FdoValue> parts = new ArrayList<>();

        if (data.length >= 1) {
            parts.add(new FdoValue.NumberValue(data[0] & 0xFF));
        }
        if (data.length >= 2) {
            parts.add(new FdoValue.NumberValue(data[1] & 0xFF));
        }
        if (data.length >= 4) {
            int maxChars = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
            parts.add(new FdoValue.NumberValue(maxChars));
        } else if (data.length >= 3) {
            parts.add(new FdoValue.NumberValue(data[2] & 0xFF));
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }
        return new FdoValue.ListValue(parts);
    }

    private static FdoValue decodeNumberList(byte[] data) {
        if (data.length == 0) {
            return FdoValue.EmptyValue.instance();
        }
        if (data.length == 1) {
            return new FdoValue.NumberValue(data[0] & 0xFF);
        }

        List<FdoValue> parts = new ArrayList<>();
        for (byte b : data) {
            parts.add(new FdoValue.NumberValue(b & 0xFF));
        }
        return new FdoValue.ListValue(parts);
    }

    private static FdoValue decodeLetter(byte[] data) {
        if (data.length == 0) {
            return new FdoValue.StringValue("A");
        }
        char letter = (char) ('A' + (data[0] & 0xFF));
        return new FdoValue.StringValue(String.valueOf(letter));
    }

    private static FdoValue decodeLetterNumber(byte[] data) {
        if (data.length < 2) {
            return decodeLetter(data);
        }
        char letter = (char) ('A' + (data[0] & 0xFF));
        long number = 0;
        for (int i = 1; i < data.length; i++) {
            number = (number << 8) | (data[i] & 0xFF);
        }

        List<FdoValue> parts = new ArrayList<>();
        parts.add(new FdoValue.StringValue(String.valueOf(letter)));
        parts.add(new FdoValue.NumberValue(number));
        return new FdoValue.ListValue(parts);
    }

    private static FdoValue decodeLetterString(byte[] data) {
        if (data.length < 2) {
            return decodeLetter(data);
        }
        char letter = (char) ('A' + (data[0] & 0xFF));
        String str = new String(data, 1, data.length - 1, StandardCharsets.ISO_8859_1);

        List<FdoValue> parts = new ArrayList<>();
        parts.add(new FdoValue.StringValue(String.valueOf(letter)));
        parts.add(new FdoValue.StringValue(str));
        return new FdoValue.ListValue(parts);
    }

    private static FdoValue decodeStyleId(byte[] data) {
        if (data.length < 3) {
            return decodeRaw(data);
        }
        int type = data[0] & 0xFF;
        int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
        return new FdoValue.GidValue(FdoGid.of(type, id));
    }

    private static FdoValue decodeArtId(byte[] data) {
        return decode3PartGid(data);
    }

    private static FdoValue decodeSmGid(byte[] data) {
        if (data.length < 3) {
            return decodeRaw(data);
        }
        int type = data[0] & 0xFF;
        int id = ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
        return new FdoValue.GidValue(FdoGid.of(type, id));
    }

    private static FdoValue decodeSmTokenArg(byte[] data) {
        if (data.length == 0) {
            return FdoValue.EmptyValue.instance();
        }

        // Check if last byte is non-printable
        int lastByte = data[data.length - 1] & 0xFF;
        boolean lastIsPrintable = lastByte >= 32 && lastByte < 127;

        if (lastIsPrintable || data.length == 1) {
            return decodeString(data);
        } else {
            // Split: string + number
            String str = new String(data, 0, data.length - 1, StandardCharsets.ISO_8859_1);
            List<FdoValue> parts = new ArrayList<>();
            parts.add(new FdoValue.StringValue(str));
            parts.add(new FdoValue.NumberValue(lastByte));
            return new FdoValue.ListValue(parts);
        }
    }

    private static String codeToObjectType(int code) {
        return switch (code) {
            // Basic object types (0-14)
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
            // Extended object types (15-28)
            case 15 -> "ruler";
            case 16 -> "root";
            case 17 -> "rich_text";
            case 18 -> "multimedia";
            case 19 -> "chart";
            case 20 -> "pictalk";
            case 21 -> "www";
            case 22 -> "split";
            case 23 -> "organizer";
            case 24 -> "tree";
            case 25 -> "tab";
            case 26 -> "progress";
            case 27 -> "toolbar";
            case 28 -> "slider";
            default -> "unknown_" + code;
        };
    }
}
