package com.atomforge.fdo;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.atom.Protocol;
import com.atomforge.fdo.binary.BinaryEncoder;
import com.atomforge.fdo.binary.FrameAwareEncoder;
import com.atomforge.fdo.binary.AtomFrame;
import com.atomforge.fdo.text.FdoParser;
import com.atomforge.fdo.text.ast.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Main facade for compiling FDO source text to binary format.
 *
 * Usage:
 * <pre>
 * FdoCompiler compiler = FdoCompiler.create();
 * byte[] binary = compiler.compile(fdoSourceText);
 * </pre>
 */
public final class FdoCompiler {

    private final AtomTable atomTable;
    private final BinaryEncoder encoder;

    private FdoCompiler(AtomTable atomTable) {
        this.atomTable = atomTable;
        this.encoder = new BinaryEncoder();
    }

    /**
     * Create a compiler with the default atom table.
     */
    public static FdoCompiler create() {
        return new FdoCompiler(AtomTable.loadDefault());
    }

    /**
     * Create a compiler with a custom atom table.
     */
    public static FdoCompiler create(AtomTable atomTable) {
        return new FdoCompiler(atomTable);
    }

    /**
     * Compile FDO source text to binary.
     *
     * @param source The FDO source text
     * @return The compiled binary data
     * @throws FdoException if compilation fails
     */
    public byte[] compile(String source) throws FdoException {
        // Parse source to AST
        StreamNode stream = FdoParser.parse(source, atomTable);

        // Convert AST to atom frames
        List<AtomFrame> frames = streamToFrames(stream);

        // Encode frames to binary
        return encoder.encode(frames);
    }

    /**
     * Compile FDO source from an input stream.
     *
     * @param input The input stream containing FDO source text
     * @return The compiled binary data
     * @throws FdoException if compilation fails
     * @throws IOException if reading fails
     */
    public byte[] compile(InputStream input) throws FdoException, IOException {
        String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        return compile(source);
    }

    /**
     * Compile FDO source from a file.
     *
     * @param file The file containing FDO source text
     * @return The compiled binary data
     * @throws FdoException if compilation fails
     * @throws IOException if reading fails
     */
    public byte[] compile(File file) throws FdoException, IOException {
        try (InputStream input = new FileInputStream(file)) {
            return compile(input);
        }
    }

    // ========== Frame-Aware Compilation (P3 Framing Support) ==========

    /**
     * Compile FDO source to binary, delivering output in frame-sized chunks.
     *
     * <p>This method is designed for P3 framing or other size-limited transport
     * protocols. Output is produced in chunks that respect frame boundaries:</p>
     *
     * <ul>
     *   <li>Atoms are never split across frame boundaries</li>
     *   <li>Large atoms use the UNI continuation protocol (atoms 4, 5, 6)</li>
     *   <li>Each frame's size will not exceed maxFrameSize</li>
     * </ul>
     *
     * <p>Example usage for P3 framing:
     * <pre>{@code
     * compiler.compileToFrames(source, 119, (frameData, index, isLast) -> {
     *     P3Frame frame = P3Frame.wrap(frameData);
     *     connection.send(frame);
     * });
     * }</pre>
     *
     * @param source       The FDO source text
     * @param maxFrameSize Maximum bytes per frame (e.g., 119 for P3)
     * @param consumer     Callback to receive each completed frame
     * @throws FdoException if compilation fails
     */
    public void compileToFrames(String source, int maxFrameSize, FrameConsumer consumer)
            throws FdoException {
        // Parse source to AST
        StreamNode stream = FdoParser.parse(source, atomTable);

        // Convert AST to atom frames
        List<AtomFrame> frames = streamToFrames(stream);

        // Encode with frame awareness
        FrameAwareEncoder frameEncoder = new FrameAwareEncoder(maxFrameSize, consumer);
        frameEncoder.encode(frames);
    }

    /**
     * Compile FDO source from an input stream to frame-sized chunks.
     *
     * @param input        The input stream containing FDO source text
     * @param maxFrameSize Maximum bytes per frame (e.g., 119 for P3)
     * @param consumer     Callback to receive each completed frame
     * @throws FdoException if compilation fails
     * @throws IOException if reading fails
     */
    public void compileToFrames(InputStream input, int maxFrameSize, FrameConsumer consumer)
            throws FdoException, IOException {
        String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        compileToFrames(source, maxFrameSize, consumer);
    }

    /**
     * Compile FDO source from a file to frame-sized chunks.
     *
     * @param file         The file containing FDO source text
     * @param maxFrameSize Maximum bytes per frame (e.g., 119 for P3)
     * @param consumer     Callback to receive each completed frame
     * @throws FdoException if compilation fails
     * @throws IOException if reading fails
     */
    public void compileToFrames(File file, int maxFrameSize, FrameConsumer consumer)
            throws FdoException, IOException {
        try (InputStream input = new FileInputStream(file)) {
            compileToFrames(input, maxFrameSize, consumer);
        }
    }

    /**
     * Compile a pre-parsed AST to frame-sized chunks.
     *
     * <p>Useful when you want to manipulate the AST before compilation.</p>
     *
     * @param stream       The parsed AST
     * @param maxFrameSize Maximum bytes per frame
     * @param consumer     Callback to receive each completed frame
     * @throws FdoException if compilation fails
     */
    public void compileToFrames(StreamNode stream, int maxFrameSize, FrameConsumer consumer)
            throws FdoException {
        List<AtomFrame> frames = streamToFrames(stream);
        FrameAwareEncoder frameEncoder = new FrameAwareEncoder(maxFrameSize, consumer);
        frameEncoder.encode(frames);
    }

    // ========== Internal Methods ==========

    /**
     * Convert a stream AST node to a list of atom frames.
     */
    private List<AtomFrame> streamToFrames(StreamNode stream) throws FdoException {
        List<AtomFrame> frames = new ArrayList<>();

        for (AtomNode atom : stream.atoms()) {
            frames.add(atomToFrame(atom));
        }

        return frames;
    }

    /**
     * Convert an atom AST node to an atom frame.
     */
    private AtomFrame atomToFrame(AtomNode atom) throws FdoException {
        // Special handling for the_unknown placeholder atoms
        // Format: the_unknown <protocol, atom_number, data...>
        if (atom.name().equals("the_unknown")) {
            return encodeUnknownAtom(atom);
        }

        if (!atom.hasDefinition()) {
            throw new FdoException(FdoException.ErrorCode.UNRECOGNIZED_ATOM,
                "Unknown atom: " + atom.name() + " at line " + atom.line());
        }

        int protocol = atom.protocol();
        int atomNumber = atom.atomNumber();
        byte[] data = encodeArgumentsToData(atom);

        return new AtomFrame(protocol, atomNumber, data);
    }

    /**
     * Encode the_unknown placeholder atoms.
     * Format: the_unknown <protocol, atom_number, data...>
     * This allows round-trip compilation of files containing atoms not in the registry.
     */
    private AtomFrame encodeUnknownAtom(AtomNode atom) throws FdoException {
        List<ArgumentNode> rawArgs = atom.arguments();

        // Arguments may be wrapped in a ListArg - unwrap if needed
        List<ArgumentNode> args;
        if (rawArgs.size() == 1 && rawArgs.get(0) instanceof ArgumentNode.ListArg listArg) {
            args = listArg.elements();
        } else {
            args = rawArgs;
        }

        if (args.size() < 2) {
            throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                "the_unknown requires at least 2 arguments: <protocol, atom_number, [data...]>, got " + args.size());
        }

        // Extract protocol
        int protocol;
        if (args.get(0) instanceof ArgumentNode.NumberArg protoArg) {
            protocol = (int) protoArg.value();
        } else {
            throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                "the_unknown first argument must be protocol number");
        }

        // Extract atom number
        int atomNumber;
        if (args.get(1) instanceof ArgumentNode.NumberArg atomArg) {
            atomNumber = (int) atomArg.value();
        } else {
            throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                "the_unknown second argument must be atom number");
        }

        // Encode remaining arguments as raw data
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 2; i < args.size(); i++) {
            ArgumentNode arg = args.get(i);
            try {
                if (arg instanceof ArgumentNode.HexArg hex) {
                    out.write(hexToBytes(hex.value()));
                } else if (arg instanceof ArgumentNode.NumberArg numArg) {
                    out.write((int) numArg.value());
                } else if (arg instanceof ArgumentNode.StringArg str) {
                    out.write(encodeString(str.value()));
                }
            } catch (IOException e) {
                // Ignore - ByteArrayOutputStream doesn't throw
            }
        }

        return new AtomFrame(protocol, atomNumber, out.toByteArray());
    }

    /**
     * Encode atom arguments to binary data.
     * This is the main dispatch method that routes to protocol/atom-specific encoders.
     */
    private byte[] encodeArgumentsToData(AtomNode atom) throws FdoException {
        if (!atom.hasArguments()) {
            return new byte[0];
        }

        List<ArgumentNode> args = atom.arguments();
        if (args.isEmpty()) {
            return new byte[0];
        }

        // Get atom definition for type-based encoding
        var def = atom.definition();
        var type = def.type();
        int protocol = atom.protocol();
        String atomName = atom.name();

        // Handle ListArg - multiple arguments encoded together
        ArgumentNode firstArg = args.get(0);
        if (firstArg instanceof ArgumentNode.ListArg listArg) {
            // Special handling for mat_size
            if (atomName.equals("mat_size")) {
                return encodeMatSizeArg(listArg);
            }
            // Special handling for mat_font_sis (font_id, size, style)
            if (atomName.equals("mat_font_sis")) {
                return encodeFontSisArg(listArg);
            }
            // Special handling for man_get_display_characteristics <width, 10>
            if (atomName.equals("man_get_display_characteristics")) {
                return encodeDisplayCharacteristicArg(listArg);
            }
            // Special handling for VAR list args (letter + number)
            if (protocol == Protocol.VAR) {
                return encodeVarListArg(listArg);
            }
            // Special handling for ALERT type atoms (e.g., async_alert <info, "message">)
            if (type == AtomType.ALERT || type == AtomType.ALERT_LEGACY) {
                return encodeAlertArg(listArg);
            }
            // Special handling for TOKEN type atoms (e.g., sm_send_token_raw <"Ki", 0-14017>)
            // GIDs with type=0 are encoded without the type byte (just ID bytes)
            if (type == AtomType.TOKEN || type == AtomType.TOKENARG) {
                return encodeTokenListArg(listArg);
            }
            // Special handling for mat_auto_complete (CRITERION_LEGACY with special encoding)
            if (atomName.equals("mat_auto_complete")) {
                return encodeAutoCompleteArg(listArg);
            }
            // Special handling for hfs_attr_checkbox_mapping (4-byte number + string)
            if (atomName.equals("hfs_attr_checkbox_mapping")) {
                return encodeHfsAttrCheckboxMappingArg(listArg);
            }
            // Special handling for hfs_attr_field_mapping and hfs_attr_variable_mapping (two 4-byte values)
            if (atomName.equals("hfs_attr_field_mapping") || atomName.equals("hfs_attr_variable_mapping")) {
                return encodeHfsDualDwordArg(listArg);
            }
            return encodeListArg(listArg, atom);
        }

        // Special handling for word-type atoms (2-byte big-endian encoding)
        if (atomName.equals("phone_port_list") || 
            atomName.equals("phone_ready_to_connect") ||
            atomName.equals("comit_reboot") ||
            atomName.equals("comit_restart")) {
            return encodeWordArg(firstArg);
        }

        // Handle multiple separate arguments (parsed as separate args, not a ListArg)
        // This happens for atoms like uni_use_last_atom_string <atom_ref, hex, hex, ...>
        if (args.size() > 1) {
            return encodeMultipleArgs(args, atom);
        }

        // Protocol-specific encoding
        return switch (protocol) {
            case Protocol.UNI -> encodeUniArg(firstArg, atomName, type);
            case Protocol.VAR -> encodeVarArg(firstArg, atomName);
            case Protocol.MAT -> encodeMatArg(firstArg, atomName, type);
            case Protocol.ACT -> encodeActArg(firstArg, atomName);
            case Protocol.IF -> encodeIfArg(firstArg, atomName);
            case Protocol.MAN -> encodeManArg(firstArg, atomName, type);
            case Protocol.FM -> encodeFmArg(firstArg, atomName);
            case Protocol.DE -> encodeDeArg(firstArg, atomName);
            case Protocol.BUF -> encodeBufArg(firstArg, atomName);
            case Protocol.HFS -> encodeHfsArg(firstArg, atomName, type);
            default -> encodeByType(firstArg, type);
        };
    }

    /**
     * Encode based on atom type (fallback for non-protocol-specific cases).
     * Note: CRITERION_LEGACY is handled specially in protocol-specific encoders
     * (e.g., mat_auto_complete in encodeMatArg), not here.
     */
    private byte[] encodeByType(ArgumentNode arg, AtomType type) throws FdoException {
        return switch (type) {
            case RAW -> encodeRawArg(arg);
            case DWORD -> encodeDwordArg(arg);
            case STRING -> encodeStringArg(arg);
            case GID -> encodeGidArg(arg);
            case OBJSTART -> encodeObjectTypeArg(arg);
            case STREAM -> encodeStreamArg(arg);
            case STREAM_LEGACY -> encodeStreamLegacyArg(arg);
            case BOOL -> encodeBoolArg(arg);
            case ORIENT -> encodeOrientArg(arg);
            case ALERT, ALERT_LEGACY -> encodeAlertArg(arg);
            case CRITERION -> encodeCriterionArg(arg);
            default -> encodeGenericArg(arg);
        };
    }

    /**
     * Encode UNI protocol arguments.
     * Handles typed data encoding.
     */
    private byte[] encodeUniArg(ArgumentNode arg, String atomName, AtomType type) throws FdoException {
        // uni_start_typed_data and uni_next_atom_typed encode charset identifiers as 2-byte codes
        if (atomName.equals("uni_start_typed_data") || atomName.equals("uni_next_atom_typed")) {
            return encodeTypedDataArg(arg);
        }
        // Fall back to type-based encoding
        return encodeByType(arg, type);
    }

    /**
     * Encode uni_start_typed_data argument (charset identifier).
     * Examples:
     *   ascii  -> 00 00
     *   latin1 -> 01 04
     */
    private byte[] encodeTypedDataArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            return typedDataToCode(id.value());
        }
        return encodeGenericArg(arg);
    }

    private byte[] typedDataToCode(String typedData) {
        return switch (typedData.toLowerCase()) {
            case "ascii" -> new byte[] { 0x00, 0x00 };
            case "latin1" -> new byte[] { 0x01, 0x04 };
            
            default -> new byte[] { 0x00, 0x00 };
        };
    }

    /**
     * Encode a generic argument - handles most common cases.
     */
    private byte[] encodeGenericArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.NumberArg num) {
            return encodeTrimmedDword(num.value());
        } else if (arg instanceof ArgumentNode.HexArg hex) {
            return hexToBytes(hex.value());
        } else if (arg instanceof ArgumentNode.StringArg str) {
            return encodeString(str.value());
        } else if (arg instanceof ArgumentNode.IdentifierArg id) {
            return encodeIdentifier(id.value());
        } else if (arg instanceof ArgumentNode.GidArg gid) {
            return encodeGidArg(arg);
        } else if (arg instanceof ArgumentNode.NestedStreamArg nested) {
            return encodeStreamArg(arg);
        }
        return new byte[0];
    }

    private byte[] encodeRawArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.HexArg hex) {
            return hexToBytes(hex.value());
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            // Single byte value
            return new byte[] { (byte) num.value() };
        } else if (arg instanceof ArgumentNode.StringArg str) {
            // RAW type can also accept strings (e.g., chat_add_user <"Username">)
            return unescapeString(str.value()).getBytes(StandardCharsets.UTF_8);
        }
        return new byte[0];
    }

    private byte[] encodeDwordArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.NumberArg num) {
            return encodeTrimmedDword(num.value());
        }
        return new byte[0];
    }

    /**
     * Encode word-type argument (2-byte big-endian).
     * Used by: phone_port_list, phone_ready_to_connect, comit_reboot, comit_restart
     */
    private byte[] encodeWordArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.NumberArg num) {
            long value = num.value();
            // Clamp to unsigned 16-bit range (0-65535)
            if (value < 0) value = 0;
            if (value > 0xFFFF) value = 0xFFFF;
            // Encode as 2-byte big-endian (unsigned short)
            return new byte[] {
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
            };
        }
        return new byte[0];
    }

    private byte[] encodeStringArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.StringArg str) {
            // Strings are NOT null-terminated in FDO - frame length determines string length
            // Use UTF-8 to preserve raw bytes
            return unescapeString(str.value()).getBytes(StandardCharsets.UTF_8);
        } else if (arg instanceof ArgumentNode.HexArg hex) {
            // STRING-typed atoms can also accept hex values
            return hexToBytes(hex.value());
        }
        return new byte[0];
    }

    /**
     * Encode ALERT type argument (e.g., async_alert <info, "message">).
     * Format: [alert_type_code] [string] [null_terminator]
     * Alert types: info=1, error=2, warning=5
     */
    private byte[] encodeAlertArg(ArgumentNode arg) throws FdoException {
        // Parser treats <info, "message"> as ObjectTypeArg (similar syntax to object types)
        if (arg instanceof ArgumentNode.ObjectTypeArg objArg) {
            int alertType = alertTypeToCode(objArg.objectType());
            String message = objArg.title();
            byte[] msgBytes = (message != null)
                ? unescapeString(message).getBytes(StandardCharsets.UTF_8)
                : new byte[0];

            // Format: [type_code] [string] (no null terminator - frame length determines end)
            byte[] result = new byte[1 + msgBytes.length];
            result[0] = (byte) alertType;
            System.arraycopy(msgBytes, 0, result, 1, msgBytes.length);
            return result;
        } else if (arg instanceof ArgumentNode.ListArg list) {
            List<ArgumentNode> elems = list.elements();
            if (elems.size() >= 2) {
                // First element is alert type (info, error, warning)
                int alertType = 0;
                ArgumentNode typeArg = elems.get(0);
                if (typeArg instanceof ArgumentNode.IdentifierArg id) {
                    alertType = alertTypeToCode(id.value());
                }

                // Second element is the message string
                ArgumentNode msgArg = elems.get(1);
                byte[] msgBytes = new byte[0];
                if (msgArg instanceof ArgumentNode.StringArg str) {
                    msgBytes = unescapeString(str.value()).getBytes(StandardCharsets.UTF_8);
                }

                // Format: [type_code] [string] (no null terminator)
                byte[] result = new byte[1 + msgBytes.length];
                result[0] = (byte) alertType;
                System.arraycopy(msgBytes, 0, result, 1, msgBytes.length);
                return result;
            }
        }
        return new byte[0];
    }

    private int alertTypeToCode(String alertType) {
        
        return switch (alertType.toLowerCase()) {
            case "info" -> 0x01;
            case "error" -> 0x02;
            case "pop_info" -> 0x03;       
            case "pop_error" -> 0x04;      
            case "warning" -> 0x05;
            case "pop_warning" -> 0x06;    
            case "yes_no" -> 0x07;
            case "yes_no_cancel" -> 0x08;
            default -> 0;
        };
    }

    /**
     * Unescape a string (handle \n, \r, \t, \", \\, \xHH).
     */
    private String unescapeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case 'x' -> {
                        if (i + 3 < str.length()) {
                            String hex = str.substring(i + 2, i + 4);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 3;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private byte[] encodeGidArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.GidArg gid) {
            int[] parts = gid.parts();
            if (parts.length == 2) {
                // 2-part: type-id -> delegate to FdoGid
                return com.atomforge.fdo.model.FdoGid.of(parts[0], parts[1]).toBytes();
            } else if (parts.length == 3) {
                
                return com.atomforge.fdo.model.FdoGid.of(parts[0], parts[1], parts[2]).toBytes();
            }
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            // Single number GID (e.g., <59>) - encode as trimmed DWORD
            return encodeTrimmedDword(num.value());
        }
        return new byte[0];
    }

    private byte[] encodeObjectTypeArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ObjectTypeArg obj) {
            int typeCode = objectTypeToCode(obj.objectType());
            String title = obj.title();

            if (title == null || title.isEmpty()) {
                return new byte[] { (byte) typeCode };
            }

            // Title is NOT null-terminated - length is implicit in frame length
            // Must unescape string to convert \x00 sequences to actual bytes
            byte[] titleBytes = unescapeString(title).getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[1 + titleBytes.length];
            result[0] = (byte) typeCode;
            System.arraycopy(titleBytes, 0, result, 1, titleBytes.length);
            return result;
        } else if (arg instanceof ArgumentNode.IdentifierArg id) {
            int typeCode = objectTypeToCode(id.value());
            return new byte[] { (byte) typeCode };
        }
        return new byte[0];
    }

    private byte[] encodeStreamArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.NestedStreamArg nested) {
            StreamNode stream = nested.stream();
            List<ArgumentNode> trailingData = nested.trailingData();
            
            // Check if this is actually an atom reference with optional trailing data
            // This happens when parser treats <atom_name, hex, hex, ...> as a nested stream
            // where atom_name is seen as an atom with "arguments"
            if (stream.atoms().size() == 1) {
                AtomNode singleAtom = stream.atoms().get(0);
                if (singleAtom.hasDefinition()) {
                    // Encode as atom reference: [proto, atom] + any arguments as raw data
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    out.write(singleAtom.protocol());
                    out.write(singleAtom.atomNumber());

                    // Encode any arguments as raw data
                    if (singleAtom.hasArguments()) {
                        try {
                            byte[] argData = encodeMultipleArgs(singleAtom.arguments(), singleAtom);
                            out.write(argData);
                        } catch (IOException e) {
                            // ByteArrayOutputStream doesn't throw
                        }
                    }
                    
                    // Encode trailing data (hex/number values after the atom reference)
                    if (!trailingData.isEmpty()) {
                        for (ArgumentNode trailingArg : trailingData) {
                            if (trailingArg instanceof ArgumentNode.HexArg hex) {
                                byte[] hexBytes = hexToBytes(hex.value());
                                out.writeBytes(hexBytes);
                            } else if (trailingArg instanceof ArgumentNode.NumberArg num) {
                                // Single byte value
                                out.write((byte) num.value());
                            }
                        }
                    }
                    
                    return out.toByteArray();
                }
            }
            // Otherwise encode as a full nested stream
            List<AtomFrame> frames = streamToFrames(stream);
            byte[] streamBytes = encoder.encode(frames);
            
            // Append trailing data if present
            if (!trailingData.isEmpty()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.writeBytes(streamBytes);
                for (ArgumentNode trailingArg : trailingData) {
                    if (trailingArg instanceof ArgumentNode.HexArg hex) {
                        byte[] hexBytes = hexToBytes(hex.value());
                        out.writeBytes(hexBytes);
                    } else if (trailingArg instanceof ArgumentNode.NumberArg num) {
                        out.write((byte) num.value());
                    }
                }
                return out.toByteArray();
            }
            
            return streamBytes;
        }
        return new byte[0];
    }

    private int objectTypeToCode(String typeName) {
        return switch (typeName.toLowerCase()) {
            // Basic object types (0-14)
            case "group", "org_group" -> 0x00;
            case "independent", "ind_group" -> 0x01;
            case "dms_list", "dynamic_multi_list" -> 0x02;
            case "sms_list", "static_multi_list" -> 0x03;
            case "dss_list", "dynamic_list" -> 0x04;
            case "sss_list", "static_list" -> 0x05;
            case "trigger" -> 0x06;
            case "ornament" -> 0x07;
            case "view" -> 0x08;
            case "edit_view", "editable_view" -> 0x09;
            case "boolean" -> 0x0A;
            case "select_boolean", "selectable_boolean" -> 0x0B;
            case "range" -> 0x0C;
            case "select_range", "selectable_range" -> 0x0D;
            case "variable" -> 0x0E;
            // Extended object types (15-28)
            case "ruler", "bad_object" -> 0x0F;
            case "root", "popup_menu" -> 0x10;
            case "rich_text", "tool_group" -> 0x11;
            case "multimedia", "tab_group" -> 0x12;
            case "chart", "tab_page" -> 0x13;
            case "pictalk" -> 0x14;
            case "www" -> 0x15;
            case "split" -> 0x16;
            case "organizer" -> 0x17;
            case "tree" -> 0x18;
            case "tab" -> 0x19;
            case "progress" -> 0x1A;
            case "toolbar" -> 0x1B;
            case "slider" -> 0x1C;
            default -> {
                // Handle unknown_N format for round-trip compatibility
                if (typeName.toLowerCase().startsWith("unknown_")) {
                    try {
                        yield Integer.parseInt(typeName.substring(8));
                    } catch (NumberFormatException e) {
                        yield 0x01;
                    }
                }
                yield 0x01; // default to independent
            }
        };
    }

    // ===== Protocol-specific encoders =====

    /**
     * Encode VAR protocol arguments.
     * VAR atoms use single letters (A=0, B=1, etc.) and letter+number combinations.
     */
    private byte[] encodeVarArg(ArgumentNode arg, String atomName) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String value = id.value();
            // Single letter: A=0, B=1, C=2, etc.
            if (value.length() == 1 && Character.isUpperCase(value.charAt(0))) {
                return new byte[] { (byte) (value.charAt(0) - 'A') };
            }
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            
            long value = num.value();
            if (value > 2147483647L) {
                value = 2147483647L;
            }
            return encodeTrimmedDword(value);
        } else if (arg instanceof ArgumentNode.StringArg str) {
            return encodeString(str.value());
        } else if (arg instanceof ArgumentNode.ObjectTypeArg objArg) {
            // Parser treats <B, "string"> as ObjectTypeArg - encode as letter + string
            String letter = objArg.objectType();
            String str = objArg.title();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Encode letter (A=0, B=1, etc.)
            if (letter.length() == 1 && Character.isUpperCase(letter.charAt(0))) {
                out.write((byte) (letter.charAt(0) - 'A'));
            }
            // Encode string WITHOUT null terminator (frame length determines end)
            if (str != null && !str.isEmpty()) {
                try {
                    out.write(unescapeString(str).getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) { /* ignore */ }
            }
            return out.toByteArray();
        }
        return encodeGenericArg(arg);
    }

    /**
     * Encode VAR protocol list argument (e.g., <A, 0> -> [0x00, 0x00]).
     * Format: letter_code (1 byte), then number as trimmed big-endian (min 1 byte).
     */
    private byte[] encodeVarListArg(ArgumentNode.ListArg listArg) throws FdoException {
        List<ArgumentNode> elems = listArg.elements();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (ArgumentNode elem : elems) {
            if (elem instanceof ArgumentNode.IdentifierArg id) {
                String value = id.value();
                if (value.length() == 1 && Character.isUpperCase(value.charAt(0))) {
                    out.write((byte) (value.charAt(0) - 'A'));
                } else {
                    // Other identifier
                    try { out.write(encodeIdentifier(value)); } catch (IOException e) { /* ignore */ }
                }
            } else if (elem instanceof ArgumentNode.NumberArg num) {
                // Numbers in VAR lists use trimmed encoding (like DWORD)
                
                long value = num.value();
                if (value > 2147483647L) {
                    value = 2147483647L;
                }
                try { out.write(encodeTrimmedDword(value)); } catch (IOException e) { /* ignore */ }
            } else if (elem instanceof ArgumentNode.HexArg hex) {
                // Hex values like "14x" or "e4x" - encode as single byte
                // Remove trailing 'x' if present and parse as hex
                String hexStr = hex.value();
                if (hexStr.endsWith("x") || hexStr.endsWith("X")) {
                    hexStr = hexStr.substring(0, hexStr.length() - 1);
                }
                // Parse as hex and encode as single byte (VAR protocol uses single-byte hex values)
                long value = Long.parseLong(hexStr, 16);
                out.write((byte) (value & 0xFF));
            } else if (elem instanceof ArgumentNode.StringArg str) {
                try { out.write(encodeString(str.value())); } catch (IOException e) { /* ignore */ }
            }
        }
        return out.toByteArray();
    }

    /**
     * Encode TOKEN type list argument (e.g., sm_send_token_raw <"Ki", 0-14017>).
     * Format: string bytes + GID bytes (for 2-part GIDs with type=0, omit the type byte).
     */
    private byte[] encodeTokenListArg(ArgumentNode.ListArg listArg) throws FdoException {
        List<ArgumentNode> elems = listArg.elements();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (ArgumentNode elem : elems) {
            if (elem instanceof ArgumentNode.StringArg str) {
                out.writeBytes(encodeString(str.value()));
            } else if (elem instanceof ArgumentNode.GidArg gid) {
                // Special handling: 2-part GIDs with type=0 encode without type byte
                int[] parts = gid.parts();
                if (parts.length == 2 && parts[0] == 0) {
                    // Encode just the ID bytes (2 bytes), not the full 3-byte GID
                    int id = parts[1];
                    out.write((byte) ((id >> 8) & 0xFF));
                    out.write((byte) (id & 0xFF));
                } else {
                    // Use normal GID encoding for other cases
                    out.writeBytes(encodeGidArg(elem));
                }
            } else if (elem instanceof ArgumentNode.NumberArg num) {
                long val = num.value();
                if (val >= 0 && val <= 255) {
                    out.write((byte) val);
                } else {
                    out.writeBytes(encodeTrimmedDword(val));
                }
            } else if (elem instanceof ArgumentNode.HexArg hex) {
                out.writeBytes(hexToBytes(hex.value()));
            } else {
                out.writeBytes(encodeGenericArg(elem));
            }
        }
        return out.toByteArray();
    }

    /**
     * Encode MAT protocol arguments.
     * Handles orientation, position, booleans, font IDs, font styles, etc.
     */
    private byte[] encodeMatArg(ArgumentNode arg, String atomName, AtomType type) throws FdoException {
        // Orientation atoms
        if (atomName.contains("orientation")) {
            return encodeOrientArg(arg);
        }
        // Position atoms
        if (atomName.contains("position")) {
            return encodePositionArg(arg);
        }
        // Text on picture position (art_middle_right | title_middle_left style flags)
        if (atomName.equals("mat_text_on_picture_pos")) {
            return encodeTextOnPicturePosArg(arg);
        }
        // Boolean atoms
        if (atomName.startsWith("mat_bool_")) {
            return encodeBoolArg(arg);
        }
        // Font ID atoms
        if (atomName.contains("font_id")) {
            return encodeFontIdArg(arg);
        }
        // Font style atoms
        if (atomName.equals("mat_font_style")) {
            return encodeFontStyleArg(arg);
        }
        // Title position
        if (atomName.equals("mat_title_pos")) {
            return encodeTitlePosArg(arg);
        }
        // Size atoms (special format: v1, v2, 00, v3)
        if (atomName.equals("mat_size")) {
            return encodeMatSizeArg(arg);
        }
        // Log object type
        if (atomName.equals("mat_log_object")) {
            return encodeLogObjectArg(arg);
        }
        // Frame style (2-byte encoding)
        if (atomName.equals("mat_frame_style")) {
            return encodeFrameStyleArg(arg);
        }
        // Trigger style (2-byte encoding)
        if (atomName.equals("mat_trigger_style")) {
            return encodeTriggerStyleArg(arg);
        }
        // Font SIS (font_id, size, style) - needs special encoding
        if (atomName.equals("mat_font_sis")) {
            return encodeFontSisArg(arg);
        }
        // Sort order
        if (atomName.equals("mat_sort_order")) {
            return encodeSortOrderArg(arg);
        }
        // Field script (character set)
        if (atomName.equals("mat_field_script")) {
            return encodeFieldScriptArg(arg);
        }
        // Title append screen name style
        if (atomName.equals("mat_title_append_screen_name")) {
            return encodeTitleAppendScreenNameArg(arg);
        }
        // Auto complete (CRITERION_LEGACY type with special encoding)
        if (atomName.equals("mat_auto_complete")) {
            return encodeAutoCompleteArg(arg);
        }
        // Default to type-based encoding
        return encodeByType(arg, type);
    }

    /**
     * Encode mat_field_script argument (character set encoding).
     */
    private byte[] encodeFieldScriptArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = fieldScriptToCode(id.value());
            return new byte[] { (byte) code };
        }
        return encodeGenericArg(arg);
    }

    private int fieldScriptToCode(String script) {
        return switch (script.toLowerCase()) {
            case "latin", "latin1" -> 0x00;
            case "japanese" -> 0x01;  
            case "chinesetr" -> 0x02;  
            case "any" -> 0x80;
            case "chineses" -> 0x19;  
            case "default" -> 0xFF;  
            case "ascii" -> 0x7F;  
            default -> 0;
        };
    }

    /**
     * Encode mat_sort_order argument.
     */
    private byte[] encodeSortOrderArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = sortOrderToCode(id.value());
            return new byte[] { (byte) code };
        }
        return encodeGenericArg(arg);
    }

    private int sortOrderToCode(String order) {
        return switch (order.toLowerCase()) {
            case "normal" -> 0x00;  
            case "reverse" -> 0x01;
            case "alphabetical" -> 0x02;
            default -> 0;
        };
    }

    /**
     * Encode mat_title_append_screen_name argument.
     */
    private byte[] encodeTitleAppendScreenNameArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = titleAppendScreenNameToCode(id.value());
            return new byte[] { (byte) code };
        }
        return encodeGenericArg(arg);
    }

    private int titleAppendScreenNameToCode(String style) {
        return switch (style.toLowerCase()) {
            case "prepend_with_s" -> 0x10;
            case "append_with_for" -> 0x02;  
            default -> 0;
        };
    }

    /**
     * Encode mat_text_on_picture_pos argument.
     * This uses piped flags: <art_position | title_position>
     */
    private byte[] encodeTextOnPicturePosArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.PipedArg piped) {
            int code = 0;
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    code |= textOnPicturePosToCode(id.value());
                }
            }
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.IdentifierArg id) {
            // Single identifier
            return new byte[] { (byte) textOnPicturePosToCode(id.value()) };
        }
        return encodeGenericArg(arg);
    }

    private int textOnPicturePosToCode(String pos) {
        
        return switch (pos.toLowerCase()) {
            case "art_middle_left" -> 0x40;
            case "art_middle_center" -> 0x00;
            case "art_middle_right" -> 0x50;
            case "title_middle_left" -> 0x04;
            case "title_middle_center" -> 0x00;
            case "title_middle_right" -> 0x05;
            case "title_lower_center" -> 0x07;
            case "title_upper_center" -> 0x02;
            default -> 0;
        };
    }

    /**
     * Encode mat_font_sis argument - font ID, size, and optional style.
     * Format: [font_id] [size] [style (optional)]
     */
    private byte[] encodeFontSisArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ListArg list) {
            List<ArgumentNode> elems = list.elements();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for (int i = 0; i < elems.size(); i++) {
                ArgumentNode elem = elems.get(i);
                if (i == 0) {
                    // Font ID (identifier -> code)
                    if (elem instanceof ArgumentNode.IdentifierArg id) {
                        out.write((byte) fontIdToCode(id.value()));
                    } else if (elem instanceof ArgumentNode.NumberArg num) {
                        out.write((byte) num.value());
                    }
                } else if (i == 1) {
                    // Font size (number)
                    if (elem instanceof ArgumentNode.NumberArg num) {
                        out.write((byte) num.value());
                    }
                } else if (i == 2) {
                    // Font style (identifier -> code, or piped flags)
                    if (elem instanceof ArgumentNode.IdentifierArg id) {
                        out.write((byte) fontStyleCodeToCode(id.value()));
                    } else if (elem instanceof ArgumentNode.NumberArg num) {
                        out.write((byte) num.value());
                    } else if (elem instanceof ArgumentNode.PipedArg piped) {
                        // Handle piped style args like <bold | underline>
                        int code = 0;
                        for (ArgumentNode part : piped.parts()) {
                            if (part instanceof ArgumentNode.IdentifierArg id) {
                                code |= fontStyleCodeToCode(id.value());
                            }
                        }
                        out.write((byte) code);
                    }
                }
            }
            return out.toByteArray();
        }
        return encodeGenericArg(arg);
    }

    private int fontIdToCode(String font) {
        return switch (font.toLowerCase()) {
            case "arial" -> 0x00;
            case "courier" -> 0x01;
            case "times_roman" -> 0x02;
            case "system" -> 0x03;
            case "fixed_system", "fixedsys" -> 0x04;
            case "ms_serif" -> 0x05;
            case "ms_sans_serif" -> 0x06;
            case "small_fonts" -> 0x07;
            case "courier_new" -> 0x08;
            case "script" -> 0x09;
            case "ms_mincho" -> 0x0A;
            case "ms_gothic" -> 0x0B;
            case "xi_ming_ti" -> 0x0C;
            case "biao_kai_ti" -> 0x0D;
            case "ming_lui_fixed" -> 0x0F;
            case "ming_lui_variable" -> 0x10;
            case "ms_hei" -> 0x11;
            case "ms_song" -> 0x12;
            default -> 0;
        };
    }

    private int fontStyleCodeToCode(String style) {
        // Font style codes for mat_font_sis
        return switch (style.toLowerCase()) {
            case "normal" -> 0x00;
            case "bold" -> 0x01;
            case "italic" -> 0x02;
            case "underline" -> 0x04;
            default -> 0;
        };
    }

    /**
     * Encode mat_trigger_style argument.
     * - Identifier args: 2-byte big-endian code
     * - Number args: single byte value
     */
    private byte[] encodeTriggerStyleArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = triggerStyleToCode(id.value());
            return new byte[] { (byte) (code >> 8), (byte) code };
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            // Numeric trigger styles encode as single byte
            return new byte[] { (byte) num.value() };
        }
        return new byte[] { 0, 0 };
    }

    private int triggerStyleToCode(String style) {
        return switch (style.toLowerCase()) {
            case "default" -> 0x0000;
            case "place" -> 0x0001;
            case "rectangle" -> 0x0002;
            case "picture" -> 0x0003;
            case "framed" -> 0x0004;  
            case "bottom_tab" -> 0x0005;
            case "plain_picture" -> 0x0006;
            case "group_state" -> 0x0007;
            default -> 0;
        };
    }

    /**
     * Encode ACT protocol arguments.
     * Handles criterion values (select=1, gain_focus=4, etc.).
     */
    private byte[] encodeActArg(ArgumentNode arg, String atomName) throws FdoException {
        // Atoms that use criterion-style encoding
        if (atomName.contains("criterion") || atomName.equals("act_do_action")) {
            return encodeCriterionArg(arg);
        }
        return encodeGenericArg(arg);
    }

    /**
     * Encode IF protocol arguments.
     * IF atoms typically take two byte values.
     */
    private byte[] encodeIfArg(ArgumentNode arg, String atomName) throws FdoException {
        // IF atoms usually have ListArg handled separately
        return encodeGenericArg(arg);
    }

    /**
     * Encode FM (File Manager) protocol arguments.
     * fm_item_type and fm_item_get use enumeration codes for file types.
     * fm_handle_error uses flag encoding.
     */
    private byte[] encodeFmArg(ArgumentNode arg, String atomName) throws FdoException {
        if (atomName.equals("fm_item_type") || atomName.equals("fm_item_get")) {
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                int code = fmItemTypeToCode(id.value());
                return new byte[] { (byte) code };
            }
        } else if (atomName.equals("fm_handle_error")) {
            return encodeFmHandleErrorArg(arg);
        }
        return encodeGenericArg(arg);
    }

    private byte[] encodeFmHandleErrorArg(ArgumentNode arg) throws FdoException {
        int flags = 0;
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            flags = fmErrorFlagToCode(id.value());
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            flags = (int) num.value();
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    flags |= fmErrorFlagToCode(id.value());
                } else if (part instanceof ArgumentNode.NumberArg num) {
                    flags |= (int) num.value();
                }
            }
        }
        return new byte[] { (byte) flags };
    }

    private int fmErrorFlagToCode(String flag) {
        return switch (flag.toLowerCase()) {
            case "display_msg" -> 0x01;
            case "terminate" -> 0x02;
            case "broadcast" -> 0x04;  
            default -> 0;
        };
    }

    private int fmItemTypeToCode(String itemType) {
        
        return switch (itemType.toLowerCase()) {
            case "file_type_id" -> 0x01;
            case "file_group_id" -> 0x02;          
            case "file_type_ext" -> 0x03;
            case "file_type_desc" -> 0x04;
            case "file_type_flags" -> 0x05;        
            case "filename" -> 0x06;
            case "path" -> 0x07;
            case "filespec" -> 0x08;
            case "handle" -> 0x09;                 
            case "error_code" -> 0x0A;
            case "custom_data" -> 0x0B;            
            case "text_width" -> 0x0C;             
            case "text_indent" -> 0x0D;            
            case "text_sub_indent" -> 0x0E;        
            case "text_flags" -> 0x0F;             
            case "dialog_flags" -> 0x10;           
            case "and_mask" -> 0x11;               
            case "xor_mask" -> 0x12;
            case "file_size" -> 0x13;              
            case "free_disk_space" -> 0x14;        
            case "broadcast_return_value" -> 0x15;
            case "file_type_list_size" -> 0x16;    
            case "print_from_page" -> 0x17;        
            case "print_to_page" -> 0x18;          
            case "print_min_page" -> 0x19;         
            case "print_max_page" -> 0x1A;         
            case "print_copies" -> 0x1B;           
            case "hdc" -> 0x1C;                    
            case "print_job_name" -> 0x1D;         
            case "date" -> 0x1E;                   
            case "time" -> 0x1F;                   
            case "text_mode" -> 0x20;              
            case "ini_string_length" -> 0x21;      
            case "ini_file" -> 0x22;
            case "ini_group" -> 0x23;
            case "ini_key" -> 0x24;
            case "ini_data_type" -> 0x25;
            case "thumbnail" -> 0x26;              
            case "persistent_path" -> 0x28;
            case "ini_section_size" -> 0x29;
            default -> 0;
        };
    }

    /**
     * Encode DE (Data Extraction) protocol arguments.
     */
    private byte[] encodeDeArg(ArgumentNode arg, String atomName) throws FdoException {
        if (atomName.equals("de_set_data_type")) {
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                int code = deDataTypeToCode(id.value());
                return new byte[] { (byte) code };
            }
        } else if (atomName.equals("de_set_extraction_type")) {
            // Uses EXTRACTMETHOD enum: default=0, current_object=1, selected_children=2, etc.
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                int code = extractMethodToCode(id.value());
                return new byte[] { (byte) code };
            }
        } else if (atomName.equals("de_get_data")) {
            // Uses EXTRACTMETHOD enum: default=0, current_object=1, selected_children=2, etc.
            
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                int code = extractMethodToCode(id.value());
                return new byte[] { (byte) code };
            }
        } else if (atomName.equals("de_get_data_value") || atomName.equals("de_get_data_pointer")) {
            // Both use EXTRACTMETHOD enum: default=0, current_object=1, selected_children=2, etc.
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                int code = extractMethodToCode(id.value());
                return new byte[] { (byte) code };
            }
        } else if (atomName.equals("de_validate")) {
            return encodeDeValidateArg(arg);
        } else if (atomName.equals("de_start_extraction")) {
            // de_start_extraction can take:
            // - No args or number like <0> -> generic encoding
            // - Piped flags like <token_header | stream_id_header | ...> -> 4-byte big-endian
            if (arg instanceof ArgumentNode.PipedArg) {
                return encodeBufFlagsArg(arg);
            }
        }
        return encodeGenericArg(arg);
    }

    private byte[] encodeDeValidateArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = deValidateToCode(id.value());
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            // Direct numeric value
            return new byte[] { (byte) num.value() };
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            int code = 0;
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    code |= deValidateToCode(id.value());
                } else if (part instanceof ArgumentNode.NumberArg num) {
                    // Numeric values are ORed directly into the flags
                    code |= num.value();
                }
            }
            return new byte[] { (byte) code };
        }
        return new byte[] { 0 };
    }

    private int deValidateToCode(String flag) {
        return switch (flag.toLowerCase()) {
            case "display_msg" -> 0x01;
            case "terminate" -> 0x02;
            default -> 0;
        };
    }

    private int deDataTypeToCode(String dataType) {
        
        return switch (dataType.toLowerCase()) {
            case "default" -> 0x00;
            case "text" -> 0x01;
            case "var" -> 0x02;
            case "boolean" -> 0x03;       
            case "global_id" -> 0x04;
            case "relative_id" -> 0x05;
            case "index" -> 0x06;
            case "child_count" -> 0x07;   
            case "objptr" -> 0x08;        
            case "value" -> 0x09;         
            case "raw" -> 0x0A;
            case "lengh", "length" -> 0x0B;  
            case "selected" -> 0x0C;      
            default -> 0;
        };
    }

    private int deDataValueToCode(String valueType) {
        return extractMethodToCode(valueType);
    }

    /**
     * Convert EXTRACTMETHOD enum identifier to code.
     */
    private int extractMethodToCode(String method) {
        return switch (method.toLowerCase()) {
            case "default" -> 0;
            case "current_object" -> 1;
            case "selected_children" -> 2;
            case "cur_obj_selected_children" -> 3;
            case "all_children" -> 4;
            case "cur_obj_all_children" -> 5;
            default -> 0;
        };
    }

    /**
     * Encode BUF (Buffer) protocol arguments.
     * buf_start_buffer and buf_use_buffer use 4-byte flag encoding.
     */
    private byte[] encodeBufArg(ArgumentNode arg, String atomName) throws FdoException {
        if (atomName.equals("buf_start_buffer") || atomName.equals("buf_use_buffer") ||
            atomName.equals("buf_set_flags") || atomName.equals("buf_get_flags")) {
            return encodeBufFlagsArg(arg);
        } else if (atomName.equals("buf_set_data_atom")) {
            // Atom reference
            if (arg instanceof ArgumentNode.IdentifierArg id) {
                var atomOpt = atomTable.findByName(id.value());
                if (atomOpt.isPresent()) {
                    var def = atomOpt.get();
                    return new byte[] { (byte) def.protocol(), (byte) def.atomNumber() };
                }
            }
        }
        return encodeGenericArg(arg);
    }

    /**
     * Encode buffer/stream legacy arguments.
     * - Numeric values use variable-length little-endian encoding
     * - Flag identifiers use 4-byte little-endian encoding
     * The frame encoder handles the length prefix.
     */
    private byte[] encodeBufFlagsArg(ArgumentNode arg) throws FdoException {
        // Handle numeric arguments with variable-length LE encoding
        if (arg instanceof ArgumentNode.NumberArg num) {
            return encodeTrimmedDwordLE(num.value());
        }

        // Handle flag identifiers - always 4-byte little-endian
        int flags = 0;

        if (arg instanceof ArgumentNode.IdentifierArg id) {
            flags = bufFlagToCode(id.value());
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    flags |= bufFlagToCode(id.value());
                } else if (part instanceof ArgumentNode.NumberArg num) {
                    flags |= (int) num.value();
                }
            }
        }

        
        return new byte[] {
            (byte) (flags >> 24),
            (byte) (flags >> 16),
            (byte) (flags >> 8),
            (byte) flags
        };
    }

    /**
     * Encode a value as trimmed DWORD in little-endian format.
     * Uses minimum bytes needed to represent the value.
     */
    private byte[] encodeTrimmedDwordLE(long value) {
        if (value == 0) {
            return new byte[] { 0 };
        }
        // Determine minimum bytes needed
        int byteCount;
        if (value <= 0xFF) {
            byteCount = 1;
        } else if (value <= 0xFFFF) {
            byteCount = 2;
        } else if (value <= 0xFFFFFF) {
            byteCount = 3;
        } else {
            byteCount = 4;
        }

        byte[] result = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    private int bufFlagToCode(String flag) {
        return switch (flag.toLowerCase()) {
            case "token_header" -> 0x00000001;
            case "stream_id_header" -> 0x00000002;
            case "host_bound" -> 0x00000004;
            case "start_stream_header" -> 0x00000008;
            case "end_stream_trailer" -> 0x00000010;
            case "data_included" -> 0x00000020;  
            case "leave_buffer_open" -> 0x00000040;  
            case "response_id_header" -> 0x00000080;
            case "pointer_included" -> 0x00000100;  
            case "clear_buffer" -> 0x00000200;  
            default -> 0;
        };
    }

    /**
     * Encode HFS protocol arguments.
     */
    private byte[] encodeHfsArg(ArgumentNode arg, String atomName, AtomType type) throws FdoException {
        // hfs_attr_flags encodes flag identifiers as 4-byte little-endian values
        if (atomName.equals("hfs_attr_flags")) {
            return encodeHfsAttrFlagsArg(arg);
        }
        // hfs_attr_database_type encodes database type identifiers as 4-byte little-endian values
        if (atomName.equals("hfs_attr_database_type")) {
            return encodeHfsAttrDatabaseTypeArg(arg);
        }
        // hfs_attr_checkbox_mapping encodes as 4-byte number + string
        if (atomName.equals("hfs_attr_checkbox_mapping")) {
            return encodeHfsAttrCheckboxMappingArg(arg);
        }
        // hfs_attr_field_mapping and hfs_attr_variable_mapping encode as two 4-byte little-endian values
        if (atomName.equals("hfs_attr_field_mapping") || atomName.equals("hfs_attr_variable_mapping")) {
            return encodeHfsDualDwordArg(arg);
        }
        // Other HFS atoms use type-based encoding
        return encodeByType(arg, type);
    }

    /**
     * Encode HFS atoms that take two 4-byte big-endian values.
     * Format: [4-byte BE value 1] [4-byte BE value 2]
     */
    private byte[] encodeHfsDualDwordArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ListArg list) {
            List<ArgumentNode> elems = list.elements();
            if (elems.size() >= 2) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                for (int i = 0; i < 2; i++) {
                    ArgumentNode elem = elems.get(i);
                    long val = 0;
                    if (elem instanceof ArgumentNode.NumberArg num) {
                        val = num.value();
                    }
                    
                    out.write((byte) (val >> 24));
                    out.write((byte) (val >> 16));
                    out.write((byte) (val >> 8));
                    out.write((byte) val);
                }
                return out.toByteArray();
            }
        }
        return encodeGenericArg(arg);
    }

    /**
     * Encode hfs_attr_flags argument.
     * Flag identifiers are encoded as 4-byte little-endian values.
     */
    private byte[] encodeHfsAttrFlagsArg(ArgumentNode arg) throws FdoException {
        int flags = 0;

        if (arg instanceof ArgumentNode.IdentifierArg id) {
            flags = hfsAttrFlagToCode(id.value());
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    flags |= hfsAttrFlagToCode(id.value());
                }
            }
        } else {
            return encodeGenericArg(arg);
        }

        
        return new byte[] {
            (byte) (flags >> 24),
            (byte) (flags >> 16),
            (byte) (flags >> 8),
            (byte) flags
        };
    }

    private int hfsAttrFlagToCode(String flag) {
        
        return switch (flag.toLowerCase()) {
            case "insert_global_id" -> 0x02;
            case "update_display" -> 0x40;
            default -> 0;
        };
    }

    /**
     * Encode hfs_attr_database_type argument.
     * Database type identifiers are encoded as 4-byte big-endian values.
     */
    private byte[] encodeHfsAttrDatabaseTypeArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int typeCode = hfsAttrDatabaseTypeToCode(id.value());
            // 4-byte big-endian encoding
            return new byte[] {
                (byte) (typeCode >> 24),
                (byte) (typeCode >> 16),
                (byte) (typeCode >> 8),
                (byte) typeCode
            };
        }
        return encodeGenericArg(arg);
    }

    private int hfsAttrDatabaseTypeToCode(String type) {
        
        return switch (type.toLowerCase()) {
            case "client" -> 0x00000000;  
            case "host" -> 0x00000001;
            default -> 0;
        };
    }

    /**
     * Encode hfs_attr_checkbox_mapping argument.
     * Format: 4-byte big-endian number + string bytes (not null-terminated).
     * Example: <1, "||"> -> 00 00 00 01 7C 7C
     *   - <6, "|||"> -> 00 00 00 06 7C 7C 7C
     *   - <15, "|"> -> 00 00 00 0F 7C
     */
    private byte[] encodeHfsAttrCheckboxMappingArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ListArg list) {
            List<ArgumentNode> elems = list.elements();
            if (elems.size() >= 2) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                
                // First element: number (always 4 bytes, big-endian)
                if (elems.get(0) instanceof ArgumentNode.NumberArg num) {
                    long value = num.value();
                    out.write((byte) (value >> 24));
                    out.write((byte) (value >> 16));
                    out.write((byte) (value >> 8));
                    out.write((byte) value);
                }
                
                // Second element: string (not null-terminated)
                if (elems.get(1) instanceof ArgumentNode.StringArg str) {
                    try {
                        out.write(encodeString(str.value()));
                    } catch (IOException e) {
                        // ByteArrayOutputStream doesn't throw
                    }
                }
                
                return out.toByteArray();
            }
        }
        return encodeGenericArg(arg);
    }

    /**
     * Encode STREAM_LEGACY type arguments.
     * STREAM_LEGACY is similar to STREAM but may have different encoding rules.
     * For now, delegate to stream encoding, but this can be customized per atom.
     */
    private byte[] encodeStreamLegacyArg(ArgumentNode arg) throws FdoException {
        // For most cases, STREAM_LEGACY encodes the same as STREAM
        // But specific atoms may override this (e.g., hfs_attr_flags handled in encodeHfsArg)
        return encodeStreamArg(arg);
    }

    /**
     * Encode MAN protocol arguments.
     */
    private byte[] encodeManArg(ArgumentNode arg, String atomName, AtomType type) throws FdoException {
        // man_set_context_globalid takes a single number
        if (atomName.equals("man_set_context_globalid")) {
            if (arg instanceof ArgumentNode.NumberArg num) {
                return new byte[] { (byte) num.value() };
            }
        }
        // man_get_display_characteristics encodes display characteristic identifiers as codes
        // Forms: <horzres> or <width, 10>
        if (atomName.equals("man_get_display_characteristics")) {
            return encodeDisplayCharacteristicArg(arg);
        }
        return encodeByType(arg, type);
    }

    /**
     * Encode man_get_display_characteristics argument.
     * Display characteristic identifiers are encoded as single byte codes.
     * Forms:
     *   - <horzres> -> [0x01]
     *   - <width, 10> -> [0x02, 0x0A]
     */
    private byte[] encodeDisplayCharacteristicArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            // Single identifier like <horzres>
            int code = displayCharacteristicToCode(id.value());
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.ListArg listArg) {
            // Identifier with value like <width, 10>
            // Format: [characteristic_code] [value] (2 bytes total)
            List<ArgumentNode> elements = listArg.elements();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (ArgumentNode elem : elements) {
                if (elem instanceof ArgumentNode.IdentifierArg id) {
                    out.write(displayCharacteristicToCode(id.value()));
                } else if (elem instanceof ArgumentNode.NumberArg num) {
                    // Value is encoded as single byte
                    out.write((int) num.value());
                }
            }
            return out.toByteArray();
        }
        return encodeGenericArg(arg);
    }

    private int displayCharacteristicToCode(String characteristic) {
        return switch (characteristic.toLowerCase()) {
            case "width" -> 0x00;
            case "height" -> 0x01;
            case "horzres" -> 0x02;
            case "vertres" -> 0x03;
            default -> 0;
        };
    }

    // ===== Specific argument type encoders =====

    /**
     * Encode a list of arguments.
     * For atoms like if_last_return_false_then <99, 100>
     */
    private byte[] encodeListArg(ArgumentNode.ListArg listArg, AtomNode atom) throws FdoException {
        List<ArgumentNode> elements = listArg.elements();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (ArgumentNode elem : elements) {
            byte[] encoded;
            if (elem instanceof ArgumentNode.NumberArg num) {
                // For list elements, numbers are typically single bytes
                long val = num.value();
                if (val >= 0 && val <= 255) {
                    encoded = new byte[] { (byte) val };
                } else {
                    encoded = encodeTrimmedDword(val);
                }
            } else if (elem instanceof ArgumentNode.HexArg hex) {
                // Hex values like "b6x" -> single byte 0xB6
                
                encoded = hexToBytes(hex.value());
            } else if (elem instanceof ArgumentNode.IdentifierArg id) {
                encoded = encodeIdentifier(id.value());
            } else if (elem instanceof ArgumentNode.StringArg str) {
                encoded = encodeString(str.value());
            } else {
                encoded = encodeGenericArg(elem);
            }
            try {
                out.write(encoded);
            } catch (IOException e) {
                throw new FdoException(FdoException.ErrorCode.BAD_ARGUMENT_FORMAT,
                    "Failed to encode list argument");
            }
        }
        return out.toByteArray();
    }

    /**
     * Encode multiple separate arguments.
     * This handles cases where the parser returns multiple args instead of a ListArg.
     * e.g., uni_use_last_atom_string <uni_command, 00x, 00x, 20x, 02x, 00x, 00x, 20x, 21x>
     * 
     * Special handling for TOKEN type atoms (e.g., sm_send_token_raw):
     * - GIDs with type=0 are encoded without the type byte (just ID bytes)
     */
    private byte[] encodeMultipleArgs(List<ArgumentNode> args, AtomNode atom) throws FdoException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var type = atom.definition().type();
        boolean isTokenType = (type == AtomType.TOKEN || type == AtomType.TOKENARG);
        
        for (ArgumentNode arg : args) {
            byte[] encoded;
            if (arg instanceof ArgumentNode.NumberArg num) {
                long val = num.value();
                if (val >= 0 && val <= 255) {
                    encoded = new byte[] { (byte) val };
                } else {
                    encoded = encodeTrimmedDword(val);
                }
            } else if (arg instanceof ArgumentNode.HexArg hex) {
                encoded = hexToBytes(hex.value());
            } else if (arg instanceof ArgumentNode.IdentifierArg id) {
                encoded = encodeIdentifier(id.value());
            } else if (arg instanceof ArgumentNode.StringArg str) {
                encoded = encodeString(str.value());
            } else if (arg instanceof ArgumentNode.GidArg gid && isTokenType) {
                // Special handling for TOKEN type: 2-part GIDs with type=0 encode without type byte
                int[] parts = gid.parts();
                if (parts.length == 2 && parts[0] == 0) {
                    // Encode just the ID bytes (2 bytes), not the full 3-byte GID
                    int id = parts[1];
                    encoded = new byte[] {
                        (byte) ((id >> 8) & 0xFF),
                        (byte) (id & 0xFF)
                    };
                } else {
                    // Use normal GID encoding for other cases
                    encoded = encodeGidArg(arg);
                }
            } else {
                encoded = encodeGenericArg(arg);
            }
            try {
                out.write(encoded);
            } catch (IOException e) {
                // ByteArrayOutputStream doesn't throw
            }
        }
        return out.toByteArray();
    }

    /**
     * Encode boolean argument (yes=1, no=0).
     */
    private byte[] encodeBoolArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String value = id.value().toLowerCase();
            if (value.equals("yes") || value.equals("true")) {
                return new byte[] { 1 };
            } else if (value.equals("no") || value.equals("false")) {
                return new byte[] { 0 };
            }
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            return new byte[] { (byte) num.value() };
        }
        return new byte[] { 0 };
    }

    /**
     * Encode orientation argument (vff, hcc, etc.).
     * The encoding is: bit 6 = v(1)/h(0), bits 5-3 = first justify, bits 2-0 = second justify
     */
    private byte[] encodeOrientArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String orient = id.value().toLowerCase();
            // Use lookup table for known orientations
            Integer code = orientationToCode(orient);
            if (code != null) {
                return new byte[] { (byte) code.intValue() };
            }
            // Fallback: compute from pattern
            if (orient.length() >= 3) {
                int computed = 0;
                char dir = orient.charAt(0);  // v or h
                char hj = orient.charAt(1);   // first justify
                char vj = orient.charAt(2);   // second justify

                if (dir == 'v') computed |= 0x40;  // bit 6 = 1 for vertical
                computed |= (justifyToCode(hj) << 3);  // bits 5-3
                computed |= justifyToCode(vj);         // bits 2-0

                return new byte[] { (byte) computed };
            }
        }
        return new byte[] { 0 };
    }

    /**
     * Lookup table for orientation codes.
     */
    private Integer orientationToCode(String orient) {
        return switch (orient) {
            case "hcc" -> 0x00;
            case "hce" -> 0x04;
            case "hcf" -> 0x03;  // computed
            case "hct" -> 0x01;  // computed
            case "hec" -> 0x20;
            case "hee" -> 0x24;
            case "hef" -> 0x23;
            case "het" -> 0x21;  // computed
            case "hfc" -> 0x18;  // computed
            case "hfe" -> 0x1C;  // computed
            case "hff" -> 0x1B;
            case "hft" -> 0x19;  // computed
            case "hlc" -> 0x08;  // computed
            case "hle" -> 0x0C;  // computed
            case "hlf" -> 0x0B;  // computed
            case "hlt" -> 0x09;  // computed
            case "hrc" -> 0x10;  // computed
            case "hrf" -> 0x13;  // computed
            case "vcf" -> 0x43;
            case "vce" -> 0x44;
            case "vcc" -> 0x40;  // computed
            case "vct" -> 0x41;  // computed
            case "vec" -> 0x60;
            case "vee" -> 0x64;
            case "vef" -> 0x63;
            case "vet" -> 0x61;  // computed
            case "vff" -> 0x5B;
            case "vft" -> 0x59;  // computed
            case "vlc" -> 0x48;  // computed
            case "vle" -> 0x4C;  // computed
            case "vlf" -> 0x4B;  // computed
            case "vlt" -> 0x49;  // computed
            case "vrf" -> 0x53;  // computed
            default -> null;
        };
    }

    private int justifyToCode(char j) {
        return switch (j) {
            case 'c' -> 0;  // center
            case 't', 'l' -> 1;  // top/left
            case 'r', 'b' -> 2;  // right/bottom
            case 'f' -> 3;  // fill
            case 'e' -> 4;  // expand/elastic
            default -> 0;
        };
    }

    /**
     * Encode position argument (center_center, top_left, etc.).
     */
    private byte[] encodePositionArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String pos = id.value().toLowerCase();
            int code = positionToCode(pos);
            return new byte[] { (byte) code };
        }
        return new byte[] { 0 };
    }

    private int positionToCode(String pos) {
        return switch (pos) {
            case "cascade" -> 0x00;
            case "top_left" -> 0x01;
            case "top_center" -> 0x02;
            case "top_right" -> 0x03;
            case "center_left" -> 0x04;
            case "center_center" -> 0x05;
            case "center_right" -> 0x06;
            case "bottom_left" -> 0x07;
            case "bottom_center" -> 0x08;
            case "bottom_right" -> 0x09;
            default -> 0x05; // default to center_center
        };
    }

    /**
     * Encode criterion argument (select=1, close=2, gain_focus=4, or numeric).
     */
    private byte[] encodeCriterionArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String value = id.value().toLowerCase();
            int code = switch (value) {
                case "void" -> 0x00;  
                case "select" -> 0x01;
                case "close" -> 0x02;
                case "open" -> 0x03;  
                case "gain_focus" -> 0x04;
                case "lose_focus" -> 0x05;  
                case "cancel" -> 0x06;  
                case "enter_free" -> 0x07;  
                case "enter_paid" -> 0x08;  
                case "create" -> 0x09;  
                case "set_online" -> 0x0A;  
                case "set_offline" -> 0x0B;  
                case "restore" -> 0x0C;  
                case "minimize" -> 0x0E;  
                case "restore_from_maximize" -> 0x0F;  
                case "restore_from_minimize" -> 0x10;  
                case "timeout" -> 0x11;  
                case "screen_name_changed" -> 0x12;  
                case "movie_over" -> 0x13;  
                case "drop" -> 0x14;  
                case "url_drop" -> 0x15;  
                case "user_delete" -> 0x16;  
                case "toggle_up" -> 0x17;  
                case "activated" -> 0x18;  
                case "deactivated" -> 0x19;  
                case "popupmenu" -> 0x1A;  
                case "destroyed" -> 0x1B;  
                default -> 0;
            };
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            // Use trimmed encoding (minimal bytes) for numeric criterion values
            return encodeTrimmedDword(num.value());
        } else if (arg instanceof ArgumentNode.HexArg hex) {
            // Hex values like c8x (0xc8 = 200) - parse hex string to number
            String hexStr = hex.value();
            // Remove trailing 'x' or 'X' if present
            if (hexStr.endsWith("x") || hexStr.endsWith("X")) {
                hexStr = hexStr.substring(0, hexStr.length() - 1);
            }
            long value = Long.parseLong(hexStr, 16);
            // Use trimmed encoding for hex criterion values
            return encodeTrimmedDword(value);
        }
        return new byte[] { 0 };
    }

    /**
     * Encode mat_auto_complete argument.
     * Format: [criterion_code1] [criterion_code2] ...
     *   - address_list -> 0x01
     *   - address_list, std_sort_search -> 0x01 0x01 (2 bytes, frame length=2)
     * Note: Just the codes, no length byte - frame encoder handles length
     */
    private byte[] encodeAutoCompleteArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ListArg listArg) {
            List<ArgumentNode> elems = listArg.elements();
            List<Byte> codes = new ArrayList<>();
            
            for (ArgumentNode elem : elems) {
                if (elem instanceof ArgumentNode.IdentifierArg id) {
                    int code = autoCompleteToCode(id.value());
                    codes.add((byte) code);
                }
            }
            
            // Format: [code1] [code2] ... (just the codes, frame encoder adds length)
            int length = codes.size();
            if (length == 0) {
                return new byte[] { 0x00 };
            }
            byte[] result = new byte[length];  // Just the codes
            for (int i = 0; i < length; i++) {
                result[i] = codes.get(i);
            }
            return result;
        } else if (arg instanceof ArgumentNode.IdentifierArg id) {
            // Single identifier: [code]
            int code = autoCompleteToCode(id.value());
            return new byte[] { (byte) code };
        }
        return new byte[] { 0x00 };
    }

    private int autoCompleteToCode(String identifier) {
        
        return switch (identifier.toLowerCase()) {
            case "web_list" -> 0x00;  
            case "address_list" -> 0x01;
            case "other_list" -> 0x02;  
            case "std_sort_search" -> 0x01;  // Even though it errors alone, it encodes as 0x01 in lists
            default -> 0x01;  // Default to 0x01 for unknown identifiers
        };
    }

    /**
     * Encode font ID argument.
     */
    private byte[] encodeFontIdArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            // Use the same font ID mapping as fontIdToCode
            int code = fontIdToCode(id.value());
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            return new byte[] { (byte) num.value() };
        }
        return new byte[] { 0 };
    }

    /**
     * Encode font style argument (bold=1, italic=2, underline=4, etc.).
     * Supports piped args like <bold | italic> which OR flags together.
     */
    private byte[] encodeFontStyleArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            return new byte[] { (byte) fontStyleToCode(id.value()) };
        } else if (arg instanceof ArgumentNode.NumberArg num) {
            return new byte[] { (byte) num.value() };
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            int code = 0;
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    code |= fontStyleToCode(id.value());
                }
            }
            return new byte[] { (byte) code };
        }
        return new byte[] { 0 };
    }

    private int fontStyleToCode(String style) {
        return switch (style.toLowerCase()) {
            case "bold" -> 1;
            case "italic" -> 2;
            case "underline" -> 4;
            case "strikeout" -> 8;
            default -> 0;
        };
    }

    /**
     * Encode mat_size argument.
     * 2 elements: <v1, v2> -> [v1, v2]
     * 3 elements: <v1, v2, v3> -> [v1, v2, v3_high, v3_low] (v3 is 16-bit big-endian)
     */
    private byte[] encodeMatSizeArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.ListArg list) {
            List<ArgumentNode> elems = list.elements();
            if (elems.size() == 2) {
                int v1 = getNumberValue(elems.get(0));
                int v2 = getNumberValue(elems.get(1));
                return new byte[] { (byte) v1, (byte) v2 };
            } else if (elems.size() == 3) {
                int v1 = getNumberValue(elems.get(0));
                int v2 = getNumberValue(elems.get(1));
                int v3 = getNumberValue(elems.get(2));
                // v3 is a 16-bit big-endian value
                return new byte[] { (byte) v1, (byte) v2, (byte) (v3 >> 8), (byte) v3 };
            }
        }
        return encodeGenericArg(arg);
    }

    private int getNumberValue(ArgumentNode arg) {
        if (arg instanceof ArgumentNode.NumberArg num) {
            return (int) num.value();
        }
        return 0;
    }

    /**
     * Encode log object type (mat_log_object).
     */
    private byte[] encodeLogObjectArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = logObjectToCode(id.value());
            return new byte[] { (byte) code };
        }
        return new byte[] { 0 };
    }

    private int logObjectToCode(String logType) {
        return switch (logType.toLowerCase()) {
            case "session_log" -> 0x00;  
            case "chat_log" -> 0x01;
            case "im_log" -> 0x02;
            case "no_log" -> 0x03;
            default -> 0;
        };
    }

    /**
     * Encode frame style (mat_frame_style) - 2-byte big-endian.
     */
    private byte[] encodeFrameStyleArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            int code = frameStyleToCode(id.value());
            return new byte[] { (byte) (code >> 8), (byte) code };
        }
        return new byte[] { 0, 0 };
    }

    private int frameStyleToCode(String style) {
        
        return switch (style.toLowerCase()) {
            case "none" -> 0x0000;                
            case "single_line_pop_out" -> 0x0001;
            case "single_line_pop_in" -> 0x0002;
            case "pop_in" -> 0x0003;
            case "pop_out" -> 0x0004;
            case "double_line" -> 0x0005;
            case "shadow" -> 0x0006;
            case "highlight" -> 0x0007;
            default -> 0;
        };
    }

    /**
     * Encode title position argument (top_or_left, bottom_or_right, center).
     */
    private byte[] encodeTitlePosArg(ArgumentNode arg) throws FdoException {
        if (arg instanceof ArgumentNode.IdentifierArg id) {
            String pos = id.value().toLowerCase();
            int code = titlePosToCode(pos);
            return new byte[] { (byte) code };
        } else if (arg instanceof ArgumentNode.PipedArg piped) {
            // Title position encoding uses bit flags:
            // bit 7 (0x80): set for "right" or "below"
            // bit 6 (0x40): set for "above" or "below"
            // If a number is included in the piped args, use it directly
            int code = 0;
            for (ArgumentNode part : piped.parts()) {
                if (part instanceof ArgumentNode.IdentifierArg id) {
                    String p = id.value().toLowerCase();
                    switch (p) {
                        case "above" -> code |= 0x40;            // bit 6
                        case "below" -> code |= 0xC0;            // bits 6+7
                        case "right" -> code |= 0x80;            // bit 7
                        case "left", "center" -> {}              // no bits
                        case "top_or_left" -> code |= 0x01;      // legacy code
                        case "bottom_or_right" -> code |= 0x02;  // legacy code
                    }
                } else if (part instanceof ArgumentNode.NumberArg num) {
                    // If a number is in the piped args, use it directly
                    code = (int) num.value();
                }
            }
            return new byte[] { (byte) code };
        }
        return new byte[] { 0 };
    }

    private int titlePosToCode(String pos) {
        // Title position encoding uses bit flags:
        // bit 7 (0x80): set for "right" or "below"
        // bit 6 (0x40): set for "above" or "below"
        // Legacy codes: top_or_left=1, bottom_or_right=2
        return switch (pos.toLowerCase()) {
            case "above" -> 0x40;           // bit 6
            case "below" -> 0xC0;           // bits 6+7
            case "right" -> 0x80;           // bit 7
            case "left", "center" -> 0;     // no bits
            case "top_or_left" -> 0x01;     // legacy
            case "bottom_or_right" -> 0x02; // legacy
            default -> 0;
        };
    }

    /**
     * Encode a generic identifier (letters, enum names, atom references).
     */
    private byte[] encodeIdentifier(String value) {
        // Single uppercase letter: A=0, B=1, etc.
        if (value.length() == 1 && Character.isUpperCase(value.charAt(0))) {
            return new byte[] { (byte) (value.charAt(0) - 'A') };
        }
        // Boolean values
        if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true")) {
            return new byte[] { 1 };
        }
        if (value.equalsIgnoreCase("no") || value.equalsIgnoreCase("false")) {
            return new byte[] { 0 };
        }
        // Check if it's an atom name reference
        var atomOpt = atomTable.findByName(value);
        if (atomOpt.isPresent()) {
            var atom = atomOpt.get();
            return new byte[] { (byte) atom.protocol(), (byte) atom.atomNumber() };
        }
        // Otherwise, treat as a raw value (might need to be null-terminated string)
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encode a DWORD, trimming leading zero bytes.
     */
    private byte[] encodeTrimmedDword(long value) {
        if (value == 0) {
            return new byte[] { 0 };
        }
        // Find how many bytes are needed
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

    /**
     * Encode a string (not null-terminated since FDO uses frame length).
     * Uses UTF-8 encoding.
     */
    private byte[] encodeString(String str) {
        return unescapeString(str).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hexToBytes(String hex) {
        // Remove trailing 'x' if present (e.g., "00x" -> "00")
        if (hex.endsWith("x")) {
            hex = hex.substring(0, hex.length() - 1);
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
