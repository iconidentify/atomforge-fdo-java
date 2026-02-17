package com.atomforge.fdo.model;

import com.atomforge.fdo.FdoException;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FdoStream object model - programmatic access to decompiled FDO data.
 */
class FdoStreamTest {

    private static final HexFormat HEX = HexFormat.of();

    /**
     * Helper to convert hex string to bytes.
     */
    private static byte[] hexToBytes(String hex) {
        return HEX.parseHex(hex.replace(" ", ""));
    }

    // ========== MVP Test: Login Frame ==========

    @Test
    void testLoginFrameDecompilation() throws FdoException {
        // Login frame: username "TOSAdvisor", password "qwer"
        // Pure FDO atom stream (no Dd/P3 frame header)
        //
        // Decompiled text:
        // uni_start_stream
        //   man_set_context_relative <1>
        //   man_set_context_index <1>
        //   de_data <"TOSAdvisor">
        //   man_end_context
        //   man_end_context
        //   man_set_context_relative <2>
        //   de_data <"qwer">
        //   man_end_context
        // uni_end_stream

        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream (proto=0, atom=1, len=0)
            "010a0400000001" +              // man_set_context_relative <1> (proto=1, atom=10, len=4)
            "010b0400000001" +              // man_set_context_index <1> (proto=1, atom=11, len=4)
            "03010a544f5341647669736f72" +  // de_data <"TOSAdvisor"> (proto=3, atom=1, len=10)
            "011d00" +                      // man_end_context (proto=1, atom=29, len=0)
            "011d00" +                      // man_end_context
            "010a0400000002" +              // man_set_context_relative <2>
            "03010471776572" +              // de_data <"qwer"> (proto=3, atom=1, len=4)
            "011d00" +                      // man_end_context
            "000200"                        // uni_end_stream (proto=0, atom=2, len=0)
        );

        FdoStream stream = FdoStream.decode(binary);

        // Verify atom count
        assertEquals(10, stream.size());

        // Get username (first de_data)
        String username = stream.findFirst("de_data").orElseThrow().getString();
        assertEquals("TOSAdvisor", username);

        // Get password (second de_data)
        List<FdoAtom> deDataAtoms = stream.findAll("de_data");
        assertEquals(2, deDataAtoms.size());
        String password = deDataAtoms.get(1).getString();
        assertEquals("qwer", password);

        // Verify atom types
        assertTrue(deDataAtoms.get(0).isString());
        assertTrue(deDataAtoms.get(1).isString());

        // Round-trip verification
        byte[] recompiled = stream.toBytes();
        assertArrayEquals(binary, recompiled, "Round-trip should produce identical binary");
    }

    // ========== Query Tests ==========

    @Test
    void testFindFirst() throws FdoException {
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream
            "03010568656c6c6f" +             // de_data <"hello">
            "000200"                        // uni_end_stream
        );

        FdoStream stream = FdoStream.decode(binary);

        assertTrue(stream.findFirst("uni_start_stream").isPresent());
        assertTrue(stream.findFirst("de_data").isPresent());
        assertTrue(stream.findFirst("uni_end_stream").isPresent());
        assertFalse(stream.findFirst("nonexistent").isPresent());
    }

    @Test
    void testFindAll() throws FdoException {
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream
            "03010568656c6c6f" +             // de_data <"hello">
            "030105776f726c64" +             // de_data <"world">
            "000200"                        // uni_end_stream
        );

        FdoStream stream = FdoStream.decode(binary);

        List<FdoAtom> deDataAtoms = stream.findAll("de_data");
        assertEquals(2, deDataAtoms.size());
        assertEquals("hello", deDataAtoms.get(0).getString());
        assertEquals("world", deDataAtoms.get(1).getString());
    }

    @Test
    void testFindByProtocol() throws FdoException {
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream (proto=0)
            "010a0400000001" +              // man_set_context_relative (proto=1)
            "03010568656c6c6f" +             // de_data (proto=3)
            "011d00" +                      // man_end_context (proto=1)
            "000200"                        // uni_end_stream (proto=0)
        );

        FdoStream stream = FdoStream.decode(binary);

        List<FdoAtom> uniAtoms = stream.findByProtocol(0);
        assertEquals(2, uniAtoms.size());

        List<FdoAtom> manAtoms = stream.findByProtocol(1);
        assertEquals(2, manAtoms.size());

        List<FdoAtom> deAtoms = stream.findByProtocol(3);
        assertEquals(1, deAtoms.size());
    }

    @Test
    void testStreamApi() throws FdoException {
        byte[] binary = hexToBytes(
            "000100" +
            "03010568656c6c6f" +
            "030105776f726c64" +
            "000200"
        );

        FdoStream stream = FdoStream.decode(binary);

        // Use Java Stream API
        long stringCount = stream.stream()
            .filter(FdoAtom::isString)
            .count();
        assertEquals(2, stringCount);

        List<String> strings = stream.stream()
            .filter(FdoAtom::isString)
            .map(FdoAtom::getString)
            .toList();
        assertEquals(List.of("hello", "world"), strings);
    }

    // ========== Value Type Tests ==========

    @Test
    void testNumberValue() throws FdoException {
        // man_set_context_relative <1> - DWORD type
        byte[] binary = hexToBytes("010a0400000001");

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertEquals("man_set_context_relative", atom.name());
        assertTrue(atom.isNumber());
        assertEquals(1L, atom.getNumber());
    }

    @Test
    void testStringValue() throws FdoException {
        // de_data <"test">
        byte[] binary = hexToBytes("03010474657374");

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertEquals("de_data", atom.name());
        assertTrue(atom.isString());
        assertEquals("test", atom.getString());
    }

    @Test
    void testEmptyValue() throws FdoException {
        // uni_start_stream (no data)
        byte[] binary = hexToBytes("000100");

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertEquals("uni_start_stream", atom.name());
        assertTrue(atom.isEmpty());
    }

    // ========== Round-trip Tests ==========

    @Test
    void testRoundTripSimple() throws FdoException {
        byte[] binary = hexToBytes("000100" + "000200");

        FdoStream stream = FdoStream.decode(binary);
        byte[] recompiled = stream.toBytes();

        assertArrayEquals(binary, recompiled);
    }

    @Test
    void testRoundTripWithString() throws FdoException {
        byte[] binary = hexToBytes("03010568656c6c6f");  // de_data <"hello">

        FdoStream stream = FdoStream.decode(binary);
        byte[] recompiled = stream.toBytes();

        assertArrayEquals(binary, recompiled);
    }

    @Test
    void testRoundTripWithNumber() throws FdoException {
        byte[] binary = hexToBytes("010a0400000001");  // man_set_context_relative <1>

        FdoStream stream = FdoStream.decode(binary);
        byte[] recompiled = stream.toBytes();

        assertArrayEquals(binary, recompiled);
    }

    // ========== Type-safe accessor Tests ==========

    @Test
    void testGetStringThrowsOnWrongType() throws FdoException {
        byte[] binary = hexToBytes("010a0400000001");  // number value

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertThrows(IllegalStateException.class, atom::getString);
    }

    @Test
    void testGetNumberThrowsOnWrongType() throws FdoException {
        byte[] binary = hexToBytes("03010474657374");  // string value

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertThrows(IllegalStateException.class, atom::getNumber);
    }

    @Test
    void testOptionalAccessors() throws FdoException {
        byte[] binary = hexToBytes("03010474657374");  // string value

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        assertTrue(atom.getStringOpt().isPresent());
        assertEquals("test", atom.getStringOpt().get());
        assertFalse(atom.getNumberOpt().isPresent());
    }

    // ========== Edge Cases ==========

    @Test
    void testEmptyBinary() throws FdoException {
        FdoStream stream = FdoStream.decode(new byte[0]);
        assertTrue(stream.isEmpty());
        assertEquals(0, stream.size());
    }

    @Test
    void testContains() throws FdoException {
        byte[] binary = hexToBytes("000100" + "000200");

        FdoStream stream = FdoStream.decode(binary);

        assertTrue(stream.contains("uni_start_stream"));
        assertTrue(stream.contains("uni_end_stream"));
        assertFalse(stream.contains("de_data"));
    }

    // ========== DOD Frame Extraction Test ==========

    /**
     * Test extraction of DOD (Data On Demand) parameters from a real frame.
     *
     * Full P3 frame: 5ab70b00281228a06668002d0001001b00001b03040020001e003c04014552c21b0204014552c21b04000002000d
     * FDO payload starts at offset 12 (after P3 header), ends before trailing 0d
     *
     * Expected decompiled structure:
     * uni_start_stream
     *   dod_start
     *   dod_form_id <0-32-30>
     *   uni_transaction_id <21320386>
     *   dod_gid <1-69-21186>
     *   dod_end
     * uni_end_stream
     */
    @Test
    void testDodParameterExtraction() throws FdoException {
        // FDO binary - verified against ADA32 API output
        // Original test was extracted from a real P3 frame which had 4-byte GID encoding,
        // but ADA32 (reference implementation) produces 3-byte encoding for type=0, subtype>0.
        // We match ADA32's encoding style.
        // Atoms decoded:
        //   00 01 00       = uni_start_stream (proto=0, atom=1, len=0)
        //   1b 00 00       = dod_start (proto=27, atom=0, len=0)
        //   1b 03 03 20001e = dod_form_id <0-32-30> (proto=27, atom=3, len=3, data=GID in 3-byte format)
        //   00 3c 04 014552c2 = uni_transaction_id <21320386> (proto=0, atom=60, len=4)
        //   1b 02 04 014552c2 = dod_gid <1-69-21186> (proto=27, atom=2, len=4, data=GID in 4-byte format)
        //   1b 04 00       = dod_end (proto=27, atom=4, len=0)
        //   00 02 00       = uni_end_stream (proto=0, atom=2, len=0)
        // Note: dod_form_id uses 3-byte GID encoding (type=0, subtype>0), dod_gid uses 4-byte (type>0)
        byte[] fdoBinary = hexToBytes(
            "0001001b00001b030320001e003c04014552c21b0204014552c21b0400000200"
        );

        FdoStream stream = FdoStream.decode(fdoBinary);

        // Verify atom count
        assertEquals(7, stream.size());

        // Verify structure
        assertEquals("uni_start_stream", stream.get(0).name());
        assertEquals("dod_start", stream.get(1).name());
        assertEquals("dod_form_id", stream.get(2).name());
        assertEquals("uni_transaction_id", stream.get(3).name());
        assertEquals("dod_gid", stream.get(4).name());
        assertEquals("dod_end", stream.get(5).name());
        assertEquals("uni_end_stream", stream.get(6).name());

        // Extract dod_form_id - should be GID "0-32-30"
        FdoAtom formIdAtom = stream.findFirst("dod_form_id").orElseThrow();
        assertTrue(formIdAtom.isGid(), "dod_form_id should be a GID");
        FdoGid formGid = formIdAtom.getGid();
        assertEquals(0, formGid.type());
        assertEquals(32, formGid.subtype());
        assertEquals(30, formGid.id());

        // Extract uni_transaction_id - should be number 21320386
        FdoAtom transactionAtom = stream.findFirst("uni_transaction_id").orElseThrow();
        assertTrue(transactionAtom.isNumber(), "uni_transaction_id should be a number");
        assertEquals(21320386L, transactionAtom.getNumber());

        // Extract dod_gid - should be GID "1-69-21186"
        FdoAtom dodGidAtom = stream.findFirst("dod_gid").orElseThrow();
        assertTrue(dodGidAtom.isGid(), "dod_gid should be a GID");
        FdoGid dodGid = dodGidAtom.getGid();
        assertEquals(1, dodGid.type());
        assertEquals(69, dodGid.subtype());
        assertEquals(21186, dodGid.id());

        // Verify round-trip
        byte[] recompiled = stream.toBytes();
        assertArrayEquals(fdoBinary, recompiled, "Round-trip should produce identical binary");
    }

    /**
     * Test DOD frame with dod_gid having subtype=0.
     *
     * This tests that 3-part GIDs with subtype=0 are correctly decoded as 3-part,
     * not collapsed to 2-part. GID "1-0-21029" must NOT become "1-21029".
     *
     * Full P3 frame: 5a2eed00281848a0666800320001001b00001b03040020001e003c04010052251b0204010052251b04000002000d
     * FDO payload starts at offset 12 (after P3 header), ends before trailing 0d
     */
    @Test
    void testDodGidWithZeroSubtype() throws FdoException {
        // FDO binary - verified against ADA32 API output
        // dod_form_id <0-32-30> uses 3-byte GID encoding: 20 00 1E
        // dod_gid <1-0-21029> uses 4-byte GID encoding: 01 00 52 25 (type>0, so full 4-byte format)
        byte[] fdoBinary = hexToBytes(
            "0001001b00001b030320001e003c04010052251b0204010052251b0400000200"
        );

        FdoStream stream = FdoStream.decode(fdoBinary);

        // Verify structure (same as first DOD test)
        assertEquals(7, stream.size());
        assertEquals("uni_start_stream", stream.get(0).name());
        assertEquals("dod_start", stream.get(1).name());
        assertEquals("dod_form_id", stream.get(2).name());
        assertEquals("uni_transaction_id", stream.get(3).name());
        assertEquals("dod_gid", stream.get(4).name());
        assertEquals("dod_end", stream.get(5).name());
        assertEquals("uni_end_stream", stream.get(6).name());

        // Extract dod_gid - should be "1-0-21029" (3-part with subtype=0)
        FdoAtom dodGidAtom = stream.findFirst("dod_gid").orElseThrow();
        assertTrue(dodGidAtom.isGid(), "dod_gid should be a GID");
        FdoGid dodGid = dodGidAtom.getGid();

        // Critical assertion: subtype MUST be 0, not -1 (which would indicate 2-part)
        assertTrue(dodGid.isThreePart(), "dod_gid should be 3-part GID, not 2-part");
        assertEquals(1, dodGid.type());
        assertEquals(0, dodGid.subtype(), "subtype must be 0, not collapsed to 2-part");
        assertEquals(21029, dodGid.id());
        assertEquals("1-0-21029", dodGid.toString(), "GID string should be 3-part format");

        // Verify round-trip
        byte[] recompiled = stream.toBytes();
        assertArrayEquals(fdoBinary, recompiled, "Round-trip should produce identical binary");
    }

    // ========== Pretty Printer Tests ==========

    @Test
    void testToTextSimple() throws FdoException {
        // uni_start_stream, uni_end_stream
        byte[] binary = hexToBytes("000100" + "000200");

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        // Should have proper indentation
        assertTrue(text.contains("uni_start_stream"), "Should contain uni_start_stream");
        assertTrue(text.contains("uni_end_stream"), "Should contain uni_end_stream");
        assertTrue(text.startsWith("uni_start_stream\n"), "Should start with uni_start_stream");
    }

    @Test
    void testToTextWithIndentation() throws FdoException {
        // uni_start_stream
        //   de_data <"hello">
        // uni_end_stream
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream
            "03010568656c6c6f" +             // de_data <"hello">
            "000200"                        // uni_end_stream
        );

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        // Verify indentation: de_data should be indented
        String[] lines = text.split("\n");
        assertEquals(3, lines.length);
        assertEquals("uni_start_stream", lines[0]);
        assertTrue(lines[1].startsWith("  "), "de_data should be indented with 2 spaces");
        assertTrue(lines[1].contains("de_data"), "Should contain de_data");
        assertEquals("uni_end_stream", lines[2]);
    }

    @Test
    void testToTextWithStringValue() throws FdoException {
        byte[] binary = hexToBytes("03010474657374");  // de_data <"test">

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        // Should have quoted string argument
        assertTrue(text.contains("\"test\""), "Should contain quoted string: " + text);
        assertTrue(text.contains("de_data <\"test\">"), "Should have proper format: " + text);
    }

    @Test
    void testToTextWithNumber() throws FdoException {
        byte[] binary = hexToBytes("010a0400000001");  // man_set_context_relative <1>

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        assertTrue(text.contains("man_set_context_relative <1>"), "Should format number: " + text);
    }

    @Test
    void testToTextWithGid() throws FdoException {
        // dod_form_id <0-32-30> - 3-byte GID encoding
        byte[] binary = hexToBytes("1b030320001e");

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        assertTrue(text.contains("dod_form_id <0-32-30>"), "Should format GID: " + text);
    }

    @Test
    void testToTextWithBoolean() throws FdoException {
        // mat_bool_sane <yes> - protocol 16 (0x10), atom 112 (0x70), BOOL type
        // Binary: proto=16, atom=112, len=1, data=0x01 (yes)
        byte[] binary = hexToBytes("10 70 01 01");

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        assertTrue(text.contains("mat_bool_sane"), "Should contain atom name: " + text);
        assertTrue(text.contains("<yes>"), "Should format boolean as yes: " + text);
    }

    @Test
    void testToTextWithObjectType() throws FdoException {
        // man_start_object <ind_group, "">
        // From sample: man_start_object is proto=1, atom=0
        // Data encoding: object_type byte + title string
        // ind_group = 1, empty title = ""
        byte[] binary = hexToBytes("010001 01");  // proto=1, atom=0, len=1, data=0x01 (ind_group, no title)

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        assertTrue(text.contains("man_start_object"), "Should contain man_start_object: " + text);
    }

    @Test
    void testToTextCustomIndent() throws FdoException {
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream
            "03010568656c6c6f" +             // de_data <"hello">
            "000200"                        // uni_end_stream
        );

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText("    ");  // 4-space indent

        String[] lines = text.split("\n");
        assertTrue(lines[1].startsWith("    "), "Should use 4-space indent");
    }

    @Test
    void testToTextLoginFrame() throws FdoException {
        // Full login frame from earlier test
        byte[] binary = hexToBytes(
            "000100" +                      // uni_start_stream
            "010a0400000001" +              // man_set_context_relative <1>
            "010b0400000001" +              // man_set_context_index <1>
            "03010a544f5341647669736f72" +  // de_data <"TOSAdvisor">
            "011d00" +                      // man_end_context
            "011d00" +                      // man_end_context
            "010a0400000002" +              // man_set_context_relative <2>
            "03010471776572" +              // de_data <"qwer">
            "011d00" +                      // man_end_context
            "000200"                        // uni_end_stream
        );

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        // Verify key content
        assertTrue(text.contains("uni_start_stream"));
        assertTrue(text.contains("de_data <\"TOSAdvisor\">"));
        assertTrue(text.contains("de_data <\"qwer\">"));
        assertTrue(text.contains("uni_end_stream"));

        // Print for visual inspection
        System.out.println("Login frame formatted:\n" + text);
    }

    @Test
    void testSingleAtomFormat() throws FdoException {
        byte[] binary = hexToBytes("03010474657374");  // de_data <"test">

        FdoStream stream = FdoStream.decode(binary);
        FdoAtom atom = stream.get(0);

        String formatted = StreamPrettyPrinter.format(atom);
        assertEquals("de_data <\"test\">", formatted);
    }

    @Test
    void testStringEscaping() throws FdoException {
        // String with special characters: "test\nline"
        // In hex: 74 65 73 74 0a 6c 69 6e 65 (test + newline + line)
        byte[] binary = hexToBytes("0301" + "09" + "746573740a6c696e65");

        FdoStream stream = FdoStream.decode(binary);
        String text = stream.toText();

        // Newline should be escaped
        assertTrue(text.contains("\\n"), "Newline should be escaped: " + text);
    }
}
