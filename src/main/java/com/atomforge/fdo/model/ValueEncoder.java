package com.atomforge.fdo.model;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.binary.BinaryEncoder;
import com.atomforge.fdo.dsl.values.*;

import java.util.Optional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Encodes FdoValue objects back to binary data for round-trip support.
 */
public final class ValueEncoder {

    private ValueEncoder() {} // utility class

    /**
     * Encode an FdoAtom back to an AtomFrame.
     * If the atom has rawData preserved (from decoding), it will be used directly
     * for round-trip fidelity. Otherwise, re-encodes from the value.
     *
     * @param atom The atom to encode
     * @return Encoded AtomFrame
     */
    public static AtomFrame encode(FdoAtom atom) throws FdoException {
        // Use rawData if available for round-trip fidelity
        if (atom.hasRawData()) {
            return new AtomFrame(atom.protocol(), atom.atomNumber(), atom.rawData());
        }

        // Otherwise re-encode from value
        byte[] data = encodeValue(atom.value(), atom.type(), atom.name(), atom.protocol());
        return new AtomFrame(atom.protocol(), atom.atomNumber(), data);
    }

    /**
     * Encode an FdoValue to binary data.
     */
    private static byte[] encodeValue(FdoValue value, AtomType type, String atomName, int protocol)
            throws FdoException {

        // Handle empty values
        if (value instanceof FdoValue.EmptyValue) {
            return new byte[0];
        }

        // Check for special atom encoding first
        byte[] specialData = encodeSpecialAtom(value, atomName, protocol);
        if (specialData != null) {
            return specialData;
        }

        // Type-based encoding
        return switch (value) {
            case FdoValue.StringValue sv -> encodeString(sv.value());
            case FdoValue.NumberValue nv -> encodeNumberForType(nv.value(), type);
            case FdoValue.GidValue gv -> encodeGid(gv.gid());
            case FdoValue.BooleanValue bv -> encodeBoolean(bv.value());
            case FdoValue.OrientationValue ov -> encodeOrientation(ov.code());
            case FdoValue.ObjectTypeValue otv -> encodeObjectType(otv.typeName(), otv.title());
            case FdoValue.StreamValue sv -> encodeStream(sv.stream());
            case FdoValue.RawValue rv -> rv.data();
            case FdoValue.ListValue lv -> encodeList(lv.elements(), type);
            case FdoValue.EmptyValue ev -> new byte[0];
        };
    }

    /**
     * Handle special atom encoding that overrides type-based encoding.
     * Returns null if no special handling needed.
     */
    private static byte[] encodeSpecialAtom(FdoValue value, String atomName, int protocol) {
        // DE protocol special handling
        if (protocol == 3) {
            // de_data is always STRING
            if (atomName.equals("de_data")) {
                if (value instanceof FdoValue.StringValue sv) {
                    return encodeString(sv.value());
                }
            }
            // de_start_extraction needs at least 1 byte (0 should encode as 0x00, not empty)
            if (atomName.equals("de_start_extraction")) {
                if (value instanceof FdoValue.NumberValue nv) {
                    long v = nv.value();
                    if (v == 0) {
                        return new byte[] { 0 };
                    }
                    return encodeTrimmedDword(v);
                }
            }
        }

        // UNI protocol - atom references (uni_use_last_atom_string/value)
        if (protocol == 0) {
            if (atomName.equals("uni_use_last_atom_string") ||
                atomName.equals("uni_use_last_atom_value")) {
                if (value instanceof FdoValue.StringValue sv) {
                    return encodeAtomRef(sv.value());
                }
            }
        }

        // MAN protocol special handling - these use DWORD (4-byte) encoding
        if (protocol == 1) {
            if (atomName.equals("man_set_context_relative") ||
                atomName.equals("man_set_context_index")) {
                if (value instanceof FdoValue.NumberValue nv) {
                    return encodeDword(nv.value());  // Fixed 4-byte encoding
                }
            }
        }

        // VAR protocol - letter encoding
        if (protocol == 12) {
            if (value instanceof FdoValue.StringValue sv && sv.value().length() == 1) {
                char c = sv.value().charAt(0);
                if (c >= 'A' && c <= 'Z') {
                    return new byte[] { (byte) (c - 'A') };
                }
            }
            if (value instanceof FdoValue.ListValue lv) {
                return encodeVarList(lv.elements());
            }
        }

        // MAT protocol special atoms
        if (protocol == 16) {
            switch (atomName) {
                case "mat_font_id":
                    if (value instanceof FdoValue.StringValue sv) {
                        return new byte[] { (byte) fontIdToCode(sv.value()) };
                    }
                    break;
                case "mat_font_style":
                    if (value instanceof FdoValue.StringValue sv) {
                        return new byte[] { (byte) fontStyleToCode(sv.value()) };
                    }
                    break;
                case "mat_position":
                    if (value instanceof FdoValue.StringValue sv) {
                        return new byte[] { (byte) positionToCode(sv.value()) };
                    }
                    break;
                case "mat_title_pos":
                    if (value instanceof FdoValue.StringValue sv) {
                        return new byte[] { (byte) titlePosToCode(sv.value()) };
                    }
                    break;
                case "mat_size":
                    if (value instanceof FdoValue.ListValue lv) {
                        return encodeMatSize(lv.elements());
                    }
                    break;
            }
        }

        // ACT protocol - criterion values
        if (protocol == 2) {
            if (value instanceof FdoValue.StringValue sv) {
                String v = sv.value();
                // Use the Criterion enum to look up the code
                Criterion crit = Criterion.fromName(v);
                if (crit != null) {
                    return new byte[] { (byte) crit.code() };
                }
                // For numeric criterion codes passed as strings (e.g., "130")
                try {
                    int code = Integer.parseInt(v);
                    return new byte[] { (byte) code };
                } catch (NumberFormatException e) {
                    // Unknown criterion name - encode as-is and hope for the best
                }
            }
        }

        return null;
    }

    // ========== Type-specific encoders ==========

    private static byte[] encodeString(String value) {
        // FDO strings are NOT null-terminated - length is frame length
        // Use ISO-8859-1 to preserve raw bytes
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] encodeNumberForType(long value, AtomType type) {
        // DWORD type uses fixed 4-byte big-endian encoding (binary format compatibility)
        if (type == AtomType.DWORD) {
            return encodeDword(value);
        }
        // Other numeric types use trimmed encoding
        return encodeTrimmedDword(value);
    }

    private static byte[] encodeDword(long value) {
        // Fixed 4-byte big-endian encoding
        return new byte[] {
            (byte) ((value >> 24) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }

    private static byte[] encodeNumber(long value) {
        // Encode as trimmed big-endian DWORD
        return encodeTrimmedDword(value);
    }

    private static byte[] encodeTrimmedDword(long value) {
        if (value == 0) {
            return new byte[0];
        }

        // Find minimum number of bytes needed
        int bytes = 0;
        long temp = value;
        while (temp != 0) {
            bytes++;
            temp >>= 8;
        }

        byte[] result = new byte[bytes];
        for (int i = bytes - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private static byte[] encodeGid(FdoGid gid) {
        return gid.toBytes();
    }

    private static byte[] encodeAtomRef(String atomName) {
        // Look up atom by name and encode as [protocol, atomNumber]
        AtomTable table = AtomTable.loadDefault();
        Optional<AtomDefinition> def = table.findByName(atomName);
        if (def.isPresent()) {
            return new byte[] {
                (byte) def.get().protocol(),
                (byte) def.get().atomNumber()
            };
        }
        // Fallback: if not found, encode as raw string (will likely fail round-trip)
        return atomName.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] encodeBoolean(boolean value) {
        return new byte[] { (byte) (value ? 1 : 0) };
    }

    private static byte[] encodeOrientation(String code) {
        if (code == null || code.length() < 3) {
            return new byte[] { 0 };
        }

        int flags = 0;

        // Vertical/horizontal (bit 6)
        if (code.charAt(0) == 'v') {
            flags |= 0x40;
        }

        // Horizontal justify (bits 5-3)
        int hj = switch (code.charAt(1)) {
            case 'c' -> 0;
            case 'l' -> 1;
            case 'r' -> 2;
            case 'f' -> 3;
            case 'e' -> 4;
            default -> 0;
        };
        flags |= (hj << 3);

        // Vertical justify (bits 2-0)
        int vj = switch (code.charAt(2)) {
            case 'c' -> 0;
            case 't' -> 1;
            case 'b' -> 2;
            case 'f' -> 3;
            case 'e' -> 4;
            default -> 0;
        };
        flags |= vj;

        return new byte[] { (byte) flags };
    }

    private static byte[] encodeObjectType(String typeName, String title) {
        int typeCode = objectTypeToCode(typeName);

        if (title == null || title.isEmpty()) {
            return new byte[] { (byte) typeCode };
        }

        byte[] titleBytes = title.getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = new byte[1 + titleBytes.length];
        result[0] = (byte) typeCode;
        System.arraycopy(titleBytes, 0, result, 1, titleBytes.length);
        return result;
    }

    private static byte[] encodeStream(FdoStream stream) throws FdoException {
        if (stream.isEmpty()) {
            return new byte[0];
        }

        java.util.List<AtomFrame> frames = new java.util.ArrayList<>();
        for (FdoAtom atom : stream.atoms()) {
            frames.add(encode(atom));
        }
        return new BinaryEncoder().encode(frames);
    }

    private static byte[] encodeList(List<FdoValue> elements, AtomType type) throws FdoException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (FdoValue elem : elements) {
            try {
                if (elem instanceof FdoValue.NumberValue nv) {
                    // For lists, encode each number as single byte unless large
                    long v = nv.value();
                    if (v >= 0 && v <= 255) {
                        out.write((byte) v);
                    } else {
                        out.write(encodeTrimmedDword(v));
                    }
                } else if (elem instanceof FdoValue.StringValue sv) {
                    out.write(encodeString(sv.value()));
                } else if (elem instanceof FdoValue.GidValue gv) {
                    out.write(encodeGid(gv.gid()));
                }
            } catch (IOException e) {
                // ByteArrayOutputStream doesn't throw
            }
        }

        return out.toByteArray();
    }

    private static byte[] encodeVarList(List<FdoValue> elements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (FdoValue elem : elements) {
            try {
                if (elem instanceof FdoValue.StringValue sv) {
                    String v = sv.value();
                    if (v.length() == 1 && v.charAt(0) >= 'A' && v.charAt(0) <= 'Z') {
                        // Letter -> byte code
                        out.write((byte) (v.charAt(0) - 'A'));
                    } else {
                        out.write(v.getBytes(StandardCharsets.ISO_8859_1));
                    }
                } else if (elem instanceof FdoValue.NumberValue nv) {
                    // For VAR protocol, numbers must be at least 1 byte
                    // (needed for var_number_set <A, 0> to encode as [0x00, 0x00])
                    long val = nv.value();
                    if (val == 0) {
                        out.write(0);
                    } else {
                        out.write(encodeTrimmedDword(val));
                    }
                }
            } catch (IOException e) {
                // ByteArrayOutputStream doesn't throw
            }
        }

        return out.toByteArray();
    }

    private static byte[] encodeMatSize(List<FdoValue> elements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < elements.size(); i++) {
            FdoValue elem = elements.get(i);
            if (elem instanceof FdoValue.NumberValue nv) {
                long v = nv.value();
                if (i < 2) {
                    // First two values are single bytes
                    out.write((byte) v);
                } else {
                    // Third value is 2-byte big-endian
                    out.write((byte) ((v >> 8) & 0xFF));
                    out.write((byte) (v & 0xFF));
                }
            }
        }

        return out.toByteArray();
    }

    // ========== Code mappings (delegate to value enums) ==========

    private static int fontIdToCode(String fontName) {
        return FontId.codeFromName(fontName);
    }

    private static int fontStyleToCode(String style) {
        return FontStyle.codeFromName(style);
    }

    private static int positionToCode(String position) {
        return Position.codeFromName(position);
    }

    private static int titlePosToCode(String titlePos) {
        return TitlePosition.codeFromName(titlePos);
    }

    private static int objectTypeToCode(String typeName) {
        // First try the enum lookup which handles standard names and common aliases
        ObjectType type = ObjectType.fromName(typeName);
        if (type != null) {
            return type.code();
        }

        // Handle additional legacy aliases not in the enum
        return switch (typeName.toLowerCase()) {
            case "bad_object" -> 15;
            case "popup_menu" -> 16;
            case "tool_group" -> 17;
            case "tab_group" -> 18;
            case "tab_page" -> 19;
            default -> {
                // Handle unknown_N format for round-trip compatibility
                if (typeName.toLowerCase().startsWith("unknown_")) {
                    try {
                        yield Integer.parseInt(typeName.substring(8));
                    } catch (NumberFormatException e) {
                        yield ObjectType.IND_GROUP.code();
                    }
                }
                yield ObjectType.IND_GROUP.code();
            }
        };
    }
}
