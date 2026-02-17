package com.atomforge.fdo.model;

/**
 * Global ID (GID) value representing FDO object references.
 *
 * <p>GIDs come in two formats:
 * <ul>
 *   <li>2-part: type-id (e.g., "32-105")</li>
 *   <li>3-part: type-subtype-id (e.g., "1-0-1329")</li>
 * </ul>
 *
 * <p>Binary encoding:
 * <ul>
 *   <li>2-part: 3 bytes [type, id_high, id_low]</li>
 *   <li>3-part 0-0-X (X &lt;= 255): 1 byte [id]</li>
 *   <li>3-part 0-0-X (X &gt; 255): 2 bytes [id_high, id_low]</li>
 *   <li>3-part 0-N-X (N &gt; 0): 3 bytes [subtype, id_high, id_low]</li>
 *   <li>3-part N-M-X (N &gt; 0): 4 bytes [type, subtype, id_high, id_low]</li>
 * </ul>
 */
public record FdoGid(int type, int subtype, int id) {

    /**
     * Create a 2-part GID (type-id) with subtype = -1 to indicate absence.
     */
    public static FdoGid of(int type, int id) {
        return new FdoGid(type, -1, id);
    }

    /**
     * Create a 3-part GID (type-subtype-id).
     */
    public static FdoGid of(int type, int subtype, int id) {
        return new FdoGid(type, subtype, id);
    }

    /**
     * Parse a GID from string format.
     *
     * @param gidString GID in "type-id" or "type-subtype-id" format
     * @return parsed FdoGid
     * @throws IllegalArgumentException if format is invalid
     */
    public static FdoGid parse(String gidString) {
        if (gidString == null || gidString.isEmpty()) {
            throw new IllegalArgumentException("GID string cannot be null or empty");
        }

        String[] parts = gidString.split("-");
        if (parts.length == 2) {
            int type = Integer.parseInt(parts[0]);
            int id = Integer.parseInt(parts[1]);
            return FdoGid.of(type, id);
        } else if (parts.length == 3) {
            int type = Integer.parseInt(parts[0]);
            int subtype = Integer.parseInt(parts[1]);
            int id = Integer.parseInt(parts[2]);
            return FdoGid.of(type, subtype, id);
        } else {
            throw new IllegalArgumentException("Invalid GID format: " + gidString);
        }
    }

    /**
     * Check if this is a 2-part GID (no subtype).
     */
    public boolean isTwoPart() {
        return subtype < 0;
    }

    /**
     * Check if this is a 3-part GID (has subtype).
     */
    public boolean isThreePart() {
        return subtype >= 0;
    }

    /**
     * Get the byte length when encoded.
     */
    public int byteLength() {
        if (isTwoPart()) {
            return 3;  // [type, id_high, id_low]
        }
        // 3-part GID
        if (type == 0 && subtype == 0) {
            // Ultra-compact: 0-0-X
            return id <= 255 ? 1 : 2;
        } else if (type == 0) {
            // Compact: 0-N-X where N > 0
            return 3;  // [subtype, id_high, id_low]
        }
        // Full: N-M-X where N > 0
        return 4;  // [type, subtype, id_high, id_low]
    }

    /**
     * Encode to bytes.
     *
     * @return 1-4 byte array depending on format
     */
    public byte[] toBytes() {
        if (isTwoPart()) {
            // 2-part: [type, id_high, id_low]
            return new byte[] {
                (byte) type,
                (byte) ((id >> 8) & 0xFF),
                (byte) (id & 0xFF)
            };
        }
        // 3-part GID
        if (type == 0 && subtype == 0) {
            // Ultra-compact: 0-0-X
            if (id <= 255) {
                return new byte[] { (byte) id };
            } else {
                return new byte[] {
                    (byte) ((id >> 8) & 0xFF),
                    (byte) (id & 0xFF)
                };
            }
        } else if (type == 0) {
            // Compact: 0-N-X where N > 0 -> [subtype, id_high, id_low] (3 bytes)
            
            return new byte[] {
                (byte) subtype,
                (byte) ((id >> 8) & 0xFF),
                (byte) (id & 0xFF)
            };
        }
        // Full: N-M-X where N > 0 -> [type, subtype, id_high, id_low] (4 bytes)
        return new byte[] {
            (byte) type,
            (byte) subtype,
            (byte) ((id >> 8) & 0xFF),
            (byte) (id & 0xFF)
        };
    }

    /**
     * Decode a 2-part GID from 3 bytes.
     */
    public static FdoGid fromBytes2Part(byte[] data, int offset) {
        int type = data[offset] & 0xFF;
        int id = ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF);
        return FdoGid.of(type, id);
    }

    /**
     * Decode a 3-part GID from 4 bytes.
     */
    public static FdoGid fromBytes3Part(byte[] data, int offset) {
        int type = data[offset] & 0xFF;
        int subtype = data[offset + 1] & 0xFF;
        int id = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
        return FdoGid.of(type, subtype, id);
    }

    @Override
    public String toString() {
        if (isTwoPart()) {
            return type + "-" + id;
        } else {
            return type + "-" + subtype + "-" + id;
        }
    }
}
