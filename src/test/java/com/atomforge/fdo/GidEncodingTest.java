package com.atomforge.fdo;

import com.atomforge.fdo.model.FdoGid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GID (Global ID) encoding to verify Ada32 compatibility.
 *
 * <p>Ada32 uses compact encoding for 3-part GIDs with type=0:
 * <ul>
 *   <li>0-0-X (X <= 255): 1 byte [id]</li>
 *   <li>0-0-X (X > 255): 2 bytes [id_high, id_low]</li>
 *   <li>0-N-X (N > 0): 4 bytes [type, subtype, id_high, id_low] (full format, Ada32 compatible)</li>
 *   <li>N-M-X (N > 0): 4 bytes [type, subtype, id_high, id_low]</li>
 * </ul>
 *
 * <p>2-part GIDs always use 3 bytes: [type, id_high, id_low]
 */
@DisplayName("GID Encoding Tests (Ada32 Compatibility)")
class GidEncodingTest {

    @Nested
    @DisplayName("3-part GIDs: 0-0-X (ultra-compact encoding)")
    class ThreePartZeroZero {

        @Test
        @DisplayName("0-0-2 encodes as 1 byte: [02]")
        void testZeroZeroSmall() {
            FdoGid gid = FdoGid.of(0, 0, 2);
            byte[] encoded = gid.toBytes();

            // Ada32 verified: 0-0-2 -> [02]
            assertArrayEquals(new byte[] { 0x02 }, encoded,
                "0-0-2 should encode as 1 byte [02], not " + bytesToHex(encoded));
            assertEquals(1, gid.byteLength(), "byteLength() should return 1");
        }

        @Test
        @DisplayName("0-0-255 encodes as 1 byte: [FF]")
        void testZeroZeroMax255() {
            FdoGid gid = FdoGid.of(0, 0, 255);
            byte[] encoded = gid.toBytes();

            assertArrayEquals(new byte[] { (byte) 0xFF }, encoded,
                "0-0-255 should encode as 1 byte [FF], not " + bytesToHex(encoded));
            assertEquals(1, gid.byteLength(), "byteLength() should return 1");
        }

        @Test
        @DisplayName("0-0-256 encodes as 2 bytes: [01 00]")
        void testZeroZeroAbove255() {
            FdoGid gid = FdoGid.of(0, 0, 256);
            byte[] encoded = gid.toBytes();

            assertArrayEquals(new byte[] { 0x01, 0x00 }, encoded,
                "0-0-256 should encode as 2 bytes [01 00], not " + bytesToHex(encoded));
            assertEquals(2, gid.byteLength(), "byteLength() should return 2");
        }

        @Test
        @DisplayName("0-0-1000 encodes as 2 bytes: [03 E8]")
        void testZeroZeroLarge() {
            FdoGid gid = FdoGid.of(0, 0, 1000);
            byte[] encoded = gid.toBytes();

            assertArrayEquals(new byte[] { 0x03, (byte) 0xE8 }, encoded,
                "0-0-1000 should encode as 2 bytes [03 E8], not " + bytesToHex(encoded));
            assertEquals(2, gid.byteLength(), "byteLength() should return 2");
        }

        @Test
        @DisplayName("0-0-65535 encodes as 2 bytes: [FF FF]")
        void testZeroZeroMaxId() {
            FdoGid gid = FdoGid.of(0, 0, 65535);
            byte[] encoded = gid.toBytes();

            assertArrayEquals(new byte[] { (byte) 0xFF, (byte) 0xFF }, encoded,
                "0-0-65535 should encode as 2 bytes [FF FF], not " + bytesToHex(encoded));
            assertEquals(2, gid.byteLength(), "byteLength() should return 2");
        }
    }

    @Nested
    @DisplayName("3-part GIDs: 0-N-X where N > 0 (compact 3-byte encoding, Ada32 compatible)")
    class ThreePartZeroNonZero {

        @Test
        @DisplayName("0-1-2 encodes as 3 bytes: [01 00 02]")
        void testZeroOneSmall() {
            FdoGid gid = FdoGid.of(0, 1, 2);
            byte[] encoded = gid.toBytes();

            // Format: [subtype=1, id_high=0, id_low=2] (3-byte compact format)
            // Verified via ADA32 API: dod_form_id <0-32-30> produces 3 bytes [20 00 1E]
            assertArrayEquals(new byte[] { 0x01, 0x00, 0x02 }, encoded,
                "0-1-2 should encode as 3 bytes [01 00 02], not " + bytesToHex(encoded));
            assertEquals(3, gid.byteLength(), "byteLength() should return 3");
        }

        @Test
        @DisplayName("0-14-49916 encodes as 3 bytes: [0E C2 FC]")
        void testZeroFourteenLarge() {
            FdoGid gid = FdoGid.of(0, 14, 49916);
            byte[] encoded = gid.toBytes();

            // Format: [subtype=14, id_high=0xC2, id_low=0xFC] (3-byte compact format)
            assertArrayEquals(new byte[] { 0x0E, (byte) 0xC2, (byte) 0xFC }, encoded,
                "0-14-49916 should encode as 3 bytes [0E C2 FC], not " + bytesToHex(encoded));
            assertEquals(3, gid.byteLength(), "byteLength() should return 3");
        }
    }

    @Nested
    @DisplayName("3-part GIDs: N-M-X where N > 0 (full 4-byte encoding)")
    class ThreePartNonZero {

        @Test
        @DisplayName("1-0-2 encodes as 4 bytes: [01 00 00 02]")
        void testOneZeroSmall() {
            FdoGid gid = FdoGid.of(1, 0, 2);
            byte[] encoded = gid.toBytes();

            // Format: [type, subtype, id_high, id_low]
            assertArrayEquals(new byte[] { 0x01, 0x00, 0x00, 0x02 }, encoded,
                "1-0-2 should encode as 4 bytes [01 00 00 02], not " + bytesToHex(encoded));
            assertEquals(4, gid.byteLength(), "byteLength() should return 4");
        }

        @Test
        @DisplayName("1-12-37269 encodes as 4 bytes")
        void testTypicalGid() {
            FdoGid gid = FdoGid.of(1, 12, 37269);
            byte[] encoded = gid.toBytes();

            // Format: [type=1, subtype=12, id_high=0x91, id_low=0x95]
            assertArrayEquals(new byte[] { 0x01, 0x0C, (byte) 0x91, (byte) 0x95 }, encoded,
                "1-12-37269 should encode as 4 bytes, not " + bytesToHex(encoded));
            assertEquals(4, gid.byteLength(), "byteLength() should return 4");
        }
    }

    @Nested
    @DisplayName("2-part GIDs: type-id (always 3 bytes)")
    class TwoPart {

        @Test
        @DisplayName("32-105 encodes as 3 bytes: [20 00 69]")
        void testTwoPartTypical() {
            FdoGid gid = FdoGid.of(32, 105);
            byte[] encoded = gid.toBytes();

            // Format: [type, id_high, id_low]
            assertArrayEquals(new byte[] { 0x20, 0x00, 0x69 }, encoded,
                "32-105 should encode as 3 bytes [20 00 69], not " + bytesToHex(encoded));
            assertEquals(3, gid.byteLength(), "byteLength() should return 3");
        }

        @Test
        @DisplayName("40-10737 encodes as 3 bytes")
        void testTwoPartLargeId() {
            FdoGid gid = FdoGid.of(40, 10737);
            byte[] encoded = gid.toBytes();

            // Format: [type=40=0x28, id_high=0x29, id_low=0xF1]
            assertArrayEquals(new byte[] { 0x28, 0x29, (byte) 0xF1 }, encoded,
                "40-10737 should encode as 3 bytes, not " + bytesToHex(encoded));
            assertEquals(3, gid.byteLength(), "byteLength() should return 3");
        }
    }

    @Nested
    @DisplayName("Round-trip: encode then decode")
    class RoundTrip {

        @Test
        @DisplayName("2-part GID round-trips correctly")
        void testTwoPartRoundTrip() {
            FdoGid original = FdoGid.of(32, 105);
            byte[] encoded = original.toBytes();
            FdoGid decoded = FdoGid.fromBytes2Part(encoded, 0);

            assertEquals(original, decoded, "2-part GID should round-trip correctly");
        }

        // Note: 3-part round-trip tests will need updated decode methods
        // after the encoding fix is implemented
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        sb.append("]");
        return sb.toString();
    }
}
