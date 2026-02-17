package com.atomforge.fdo.tools;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.atom.AtomType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser for ADA.BIN binary files containing atom definitions.
 *
 * The ADA.BIN file uses a FAF (FDO Archive Format) wrapper with these markers:
 * - 0xFFED XX XX = 16-bit big-endian value (atom#, type, flags, name_length)
 * - 0xFFEA = pointer/offset data
 * - 0xFFEB = data block marker
 *
 * Atom entry pattern before each name:
 * [ffed atom_number] [ffed type_code] [ffed flags] ... [ffed name_length] &lt;name&gt;
 */
public class AdaBinParser {

    // Known atom name prefixes and their protocol numbers
    private static final Map<String, Integer> PREFIX_TO_PROTOCOL = new LinkedHashMap<>();

    static {
        // Wire protocols (0-31)
        PREFIX_TO_PROTOCOL.put("uni_", 0);
        PREFIX_TO_PROTOCOL.put("man_", 1);
        PREFIX_TO_PROTOCOL.put("act_", 2);
        PREFIX_TO_PROTOCOL.put("de_", 3);
        PREFIX_TO_PROTOCOL.put("buf_", 4);
        PREFIX_TO_PROTOCOL.put("idb_", 5);
        PREFIX_TO_PROTOCOL.put("xfer_", 7);
        PREFIX_TO_PROTOCOL.put("fm_", 8);
        PREFIX_TO_PROTOCOL.put("lm_", 9);
        PREFIX_TO_PROTOCOL.put("cm_", 10);
        PREFIX_TO_PROTOCOL.put("chat_", 11);
        PREFIX_TO_PROTOCOL.put("var_", 12);
        PREFIX_TO_PROTOCOL.put("async_", 13);
        PREFIX_TO_PROTOCOL.put("sm_", 14);
        PREFIX_TO_PROTOCOL.put("if_", 15);
        PREFIX_TO_PROTOCOL.put("mat_", 16);
        PREFIX_TO_PROTOCOL.put("mip_", 17);
        PREFIX_TO_PROTOCOL.put("reg_", 18);
        PREFIX_TO_PROTOCOL.put("font_", 19);
        PREFIX_TO_PROTOCOL.put("mmi_", 20);
        PREFIX_TO_PROTOCOL.put("imgxfer_", 21);
        PREFIX_TO_PROTOCOL.put("image_", 22);
        PREFIX_TO_PROTOCOL.put("chart_", 23);
        PREFIX_TO_PROTOCOL.put("morg_", 24);
        PREFIX_TO_PROTOCOL.put("rich_", 25);
        PREFIX_TO_PROTOCOL.put("exapi_", 26);
        PREFIX_TO_PROTOCOL.put("dod_", 27);
        PREFIX_TO_PROTOCOL.put("radio_", 28);
        PREFIX_TO_PROTOCOL.put("pictalk_", 29);
        PREFIX_TO_PROTOCOL.put("irc_", 30);
        PREFIX_TO_PROTOCOL.put("doc_", 31);

        // Extended protocols (32-127, client-local)
        PREFIX_TO_PROTOCOL.put("vid_", 32);
        PREFIX_TO_PROTOCOL.put("video_", 32);
        PREFIX_TO_PROTOCOL.put("snd_", 33);
        PREFIX_TO_PROTOCOL.put("ccl_", 34);
        PREFIX_TO_PROTOCOL.put("p3_", 35);
        PREFIX_TO_PROTOCOL.put("stats_", 36);
        PREFIX_TO_PROTOCOL.put("pt_", 37);
        PREFIX_TO_PROTOCOL.put("pakman_", 38);
        PREFIX_TO_PROTOCOL.put("ad_", 39);
        PREFIX_TO_PROTOCOL.put("app_", 40);
        PREFIX_TO_PROTOCOL.put("context_", 41);
        PREFIX_TO_PROTOCOL.put("mt_", 42);
        PREFIX_TO_PROTOCOL.put("master_", 42);
        PREFIX_TO_PROTOCOL.put("dbres_", 43);
        PREFIX_TO_PROTOCOL.put("modem_", 45);
        PREFIX_TO_PROTOCOL.put("tcp_", 46);
        PREFIX_TO_PROTOCOL.put("vrm_", 47);
        PREFIX_TO_PROTOCOL.put("www_", 48);
        PREFIX_TO_PROTOCOL.put("aolsock_", 49);
        PREFIX_TO_PROTOCOL.put("ppp_", 50);
        PREFIX_TO_PROTOCOL.put("hfs_", 51);
        PREFIX_TO_PROTOCOL.put("blank_", 52);
        PREFIX_TO_PROTOCOL.put("startup_", 54);
        PREFIX_TO_PROTOCOL.put("fax_", 55);
    }

    // Type code to AtomType mapping - codes from ADA.BIN
    private static final Map<Integer, AtomType> TYPE_CODE_MAP = new HashMap<>();

    static {
        TYPE_CODE_MAP.put(0x01, AtomType.RAW);
        TYPE_CODE_MAP.put(0x02, AtomType.DWORD);
        TYPE_CODE_MAP.put(0x03, AtomType.STRING);
        TYPE_CODE_MAP.put(0x04, AtomType.BOOL);
        TYPE_CODE_MAP.put(0x05, AtomType.GID);
        TYPE_CODE_MAP.put(0x06, AtomType.STREAM);
        TYPE_CODE_MAP.put(0x07, AtomType.OBJSTART);
        TYPE_CODE_MAP.put(0x08, AtomType.ORIENT);
        TYPE_CODE_MAP.put(0x09, AtomType.TOKEN);
        TYPE_CODE_MAP.put(0x0A, AtomType.TOKENARG);
        TYPE_CODE_MAP.put(0x0B, AtomType.ALERT);
        TYPE_CODE_MAP.put(0x0C, AtomType.ATOM);
        TYPE_CODE_MAP.put(0x0D, AtomType.COLORDATA);
        TYPE_CODE_MAP.put(0x0E, AtomType.IGNORE);
        TYPE_CODE_MAP.put(0x0F, AtomType.VARLOOKUP);
        TYPE_CODE_MAP.put(0x10, AtomType.CRITERION);
        TYPE_CODE_MAP.put(0x11, AtomType.BOOL_LEGACY);
        TYPE_CODE_MAP.put(0x12, AtomType.CRITERION_LEGACY);
        TYPE_CODE_MAP.put(0x13, AtomType.STREAM_LEGACY);
        TYPE_CODE_MAP.put(0x14, AtomType.VAR);
        TYPE_CODE_MAP.put(0x15, AtomType.VARDWORD);
        TYPE_CODE_MAP.put(0x16, AtomType.VARSTRING);
        TYPE_CODE_MAP.put(0x17, AtomType.BYTELIST);
        TYPE_CODE_MAP.put(0x18, AtomType.ALERT_LEGACY);
    }

    /**
     * Parse ADA.BIN file and return list of atom definitions.
     */
    public List<AtomDefinition> parse(Path adaBinPath) throws IOException {
        byte[] data = Files.readAllBytes(adaBinPath);
        return parse(data);
    }

    /**
     * Parse ADA.BIN data and return list of atom definitions.
     */
    public List<AtomDefinition> parse(byte[] data) {
        List<AtomDefinition> atoms = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        int i = 0;
        while (i < data.length - 4) {
            // Look for 0xFFED marker followed by name length
            if (isMarker(data, i, 0xED)) {
                int nameLen = readBigEndianWord(data, i + 2);

                // Reasonable atom name length (4-64 chars)
                if (nameLen >= 4 && nameLen <= 64) {
                    int nameStart = i + 4;
                    int nameEnd = nameStart + nameLen;

                    if (nameEnd <= data.length) {
                        String name = extractName(data, nameStart, nameLen);

                        if (name != null && isValidAtomName(name) && !seenNames.contains(name)) {
                            // Extract metadata from preceding markers
                            int[] metadata = extractMetadata(data, i);

                            if (metadata != null) {
                                int atomNumber = metadata[0];
                                int typeCode = metadata[1];
                                int flagBits = metadata[2];

                                // Map to protocol from name prefix
                                int protocol = getProtocolFromName(name);

                                // Map type code to AtomType
                                AtomType type = TYPE_CODE_MAP.getOrDefault(typeCode, AtomType.RAW);

                                // Build flags
                                EnumSet<AtomFlags> flags = EnumSet.noneOf(AtomFlags.class);
                                if ((flagBits & 0x01) != 0) flags.add(AtomFlags.INDENT);
                                if ((flagBits & 0x02) != 0) flags.add(AtomFlags.OUTDENT);
                                if ((flagBits & 0x04) != 0) flags.add(AtomFlags.EOS);

                                atoms.add(new AtomDefinition(protocol, atomNumber, name, type, flags));
                                seenNames.add(name);
                            }

                            // Skip past this name
                            i = nameEnd;
                            continue;
                        }
                    }
                }
            }
            i++;
        }

        return atoms;
    }

    private boolean isMarker(byte[] data, int pos, int markerLowByte) {
        return pos + 1 < data.length &&
               (data[pos] & 0xFF) == 0xFF &&
               (data[pos + 1] & 0xFF) == markerLowByte;
    }

    private int readBigEndianWord(byte[] data, int pos) {
        if (pos + 1 >= data.length) return -1;
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    private String extractName(byte[] data, int start, int length) {
        if (start + length > data.length) return null;

        try {
            byte[] nameBytes = new byte[length];
            System.arraycopy(data, start, nameBytes, 0, length);
            return new String(nameBytes, "ASCII");
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidAtomName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (!name.contains("_")) return false;

        // Check if starts with a known prefix
        for (String prefix : PREFIX_TO_PROTOCOL.keySet()) {
            if (name.startsWith(prefix)) {
                // Rest should be alphanumeric with underscores
                String rest = name.substring(prefix.length());
                return rest.matches("[a-z0-9_]+");
            }
        }
        return false;
    }

    /**
     * Extract metadata (atom number, type, flags) from the markers preceding the name.
     * Pattern: [ffed atom#] [ffed type] [ffed flags] ... [ffed name_len]
     */
    private int[] extractMetadata(byte[] data, int nameMarkerPos) {
        // Search backwards for up to 3 ffed markers within 50 bytes
        List<Integer> values = new ArrayList<>();
        int searchLimit = Math.max(0, nameMarkerPos - 50);

        int j = nameMarkerPos - 4;
        while (j >= searchLimit && values.size() < 3) {
            if (isMarker(data, j, 0xED)) {
                int val = readBigEndianWord(data, j + 2);
                values.add(0, val);
                j -= 4;
            } else {
                j--;
            }
        }

        if (values.size() >= 3) {
            return new int[] { values.get(0), values.get(1), values.get(2) };
        }

        return null;
    }

    private int getProtocolFromName(String name) {
        for (Map.Entry<String, Integer> entry : PREFIX_TO_PROTOCOL.entrySet()) {
            if (name.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0; // Default to UNI if unknown
    }

    /**
     * Get the known protocol prefixes and their numbers.
     */
    public static Map<String, Integer> getProtocolPrefixes() {
        return Collections.unmodifiableMap(PREFIX_TO_PROTOCOL);
    }
}
