package com.atomforge.fdo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the_unknown atom support in compiler and decompiler.
 */
public class TheUnknownTest {

    @Test
    public void testCompileTheUnknown() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        
        // Test single-byte data (using truly unknown atom)
        String source1 = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x>\nuni_end_stream <>";
        byte[] binary1 = compiler.compile(source1);
        assertNotNull(binary1);
        assertTrue(binary1.length > 0);
        
        // Test multi-byte data
        String source2 = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x, 02x, 03x>\nuni_end_stream <>";
        byte[] binary2 = compiler.compile(source2);
        assertNotNull(binary2);
        assertTrue(binary2.length > 0);
        
        // Test empty data
        String source3 = "uni_start_stream <00x>\n  the_unknown <16, 255>\nuni_end_stream <>";
        byte[] binary3 = compiler.compile(source3);
        assertNotNull(binary3);
        assertTrue(binary3.length > 0);
    }

    @Test
    public void testDecompileUnknownAtom() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Use a truly unknown atom (protocol 16, atom 255 - likely not in registry)
        String source = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x>\nuni_end_stream <>";
        byte[] binary = compiler.compile(source);
        
        // Decompile - should output the_unknown format
        String decompiled = decompiler.decompile(binary);
        assertNotNull(decompiled);
        assertTrue(decompiled.contains("the_unknown"), 
            "Decompiled output should contain 'the_unknown': " + decompiled);
        assertTrue(decompiled.contains("16"), 
            "Decompiled output should contain protocol '16': " + decompiled);
        assertTrue(decompiled.contains("255"), 
            "Decompiled output should contain atom '255': " + decompiled);
        assertTrue(decompiled.contains("01x"), 
            "Decompiled output should contain data '01x': " + decompiled);
    }

    @Test
    public void testRoundTripUnknownAtom() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Use a truly unknown atom (protocol 16, atom 255)
        String original = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x>\nuni_end_stream <>";
        
        // Compile
        byte[] binary = compiler.compile(original);
        
        // Decompile
        String decompiled = decompiler.decompile(binary);
        
        // Re-compile the decompiled output
        byte[] binary2 = compiler.compile(decompiled);
        
        // Binary should be identical (round-trip)
        assertArrayEquals(binary, binary2, 
            "Round-trip failed. Original: " + original + ", Decompiled: " + decompiled);
    }

    @Test
    public void testRoundTripMultiByteUnknownAtom() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Use a truly unknown atom with multi-byte data
        String original = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x, 02x, 03x>\nuni_end_stream <>";
        
        // Compile
        byte[] binary = compiler.compile(original);
        
        // Decompile
        String decompiled = decompiler.decompile(binary);
        
        // Re-compile the decompiled output
        byte[] binary2 = compiler.compile(decompiled);
        
        // Binary should be identical (round-trip)
        assertArrayEquals(binary, binary2, 
            "Round-trip failed. Original: " + original + ", Decompiled: " + decompiled);
    }

    @Test
    public void testRoundTripEmptyDataUnknownAtom() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Use a truly unknown atom with no data
        String original = "uni_start_stream <00x>\n  the_unknown <16, 255>\nuni_end_stream <>";
        
        // Compile
        byte[] binary = compiler.compile(original);
        
        // Decompile
        String decompiled = decompiler.decompile(binary);
        
        // Re-compile the decompiled output
        byte[] binary2 = compiler.compile(decompiled);
        
        // Binary should be identical (round-trip)
        assertArrayEquals(binary, binary2, 
            "Round-trip failed. Original: " + original + ", Decompiled: " + decompiled);
    }

    @Test
    public void testPreserveUnknownFormat() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        FdoDecompiler preserveDecompiler = FdoDecompiler.create(true);
        
        // Use a truly unknown atom
        String source = "uni_start_stream <00x>\n  the_unknown <16, 255, 01x>\nuni_end_stream <>";
        byte[] binary = compiler.compile(source);
        
        // Normal decompiler - should output the_unknown (atom not in registry)
        String normal = decompiler.decompile(binary);
        assertTrue(normal.contains("the_unknown"), 
            "Normal decompiler should output 'the_unknown': " + normal);
        
        // Preserve decompiler - should also output the_unknown
        String preserved = preserveDecompiler.decompile(binary);
        assertTrue(preserved.contains("the_unknown"), 
            "Preserve decompiler should output 'the_unknown': " + preserved);
    }

    @Test
    public void testTheUnknownWithDifferentProtocols() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Test with different protocols using truly unknown atoms
        // Protocol 0, atom 0 is uni_void (known), so use atom 200 (likely unknown)
        // Protocol 1, atom 10 might be known, so use atom 200
        // Protocol 2, atom 5 might be known, so use atom 200
        String[] sources = {
            "uni_start_stream <00x>\n  the_unknown <0, 200, 00x>\nuni_end_stream <>",
            "uni_start_stream <00x>\n  the_unknown <1, 200, 42x>\nuni_end_stream <>",
            "uni_start_stream <00x>\n  the_unknown <2, 200, 01x, 02x>\nuni_end_stream <>",
        };
        
        for (String source : sources) {
            byte[] binary = compiler.compile(source);
            String decompiled = decompiler.decompile(binary);
            
            // Should contain the_unknown format
            assertTrue(decompiled.contains("the_unknown"), 
                "Decompiled output should contain the_unknown: " + decompiled);
            
            // Round-trip test
            byte[] binary2 = compiler.compile(decompiled);
            assertArrayEquals(binary, binary2, 
                "Round-trip failed for source: " + source + ", decompiled: " + decompiled);
        }
    }

    @Test
    public void testKnownAtomUsesCanonicalName() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler decompiler = FdoDecompiler.create();
        
        // Use a known atom (protocol 16, atom 204 = mat_use_style_guide)
        // When compiled as the_unknown, it should decompile to canonical name
        String source = "uni_start_stream <00x>\n  the_unknown <16, 204, 01x>\nuni_end_stream <>";
        byte[] binary = compiler.compile(source);
        
        // Decompile - should output canonical name (not the_unknown)
        String decompiled = decompiler.decompile(binary);
        assertNotNull(decompiled);
        // Should contain the canonical name
        assertTrue(decompiled.contains("mat_use_style_guide") || decompiled.contains("use_style_guide"),
            "Known atom should decompile to canonical name, not the_unknown. Output: " + decompiled);
        
        // Round-trip should still work
        byte[] binary2 = compiler.compile(decompiled);
        assertArrayEquals(binary, binary2, 
            "Round-trip failed for known atom. Decompiled: " + decompiled);
    }

    @Test
    public void testPreserveModeForcesTheUnknown() throws FdoException {
        FdoCompiler compiler = FdoCompiler.create();
        FdoDecompiler normalDecompiler = FdoDecompiler.create(false);
        FdoDecompiler preserveDecompiler = FdoDecompiler.create(true);
        
        // Use a known atom (protocol 16, atom 204 = mat_use_style_guide)
        String source = "uni_start_stream <00x>\n  the_unknown <16, 204, 01x>\nuni_end_stream <>";
        byte[] binary = compiler.compile(source);
        
        // Normal decompiler - should output canonical name
        String normal = normalDecompiler.decompile(binary);
        assertTrue(normal.contains("mat_use_style_guide") || normal.contains("use_style_guide"),
            "Normal decompiler should output canonical name: " + normal);
        
        // Preserve decompiler - should output the_unknown format
        String preserved = preserveDecompiler.decompile(binary);
        assertTrue(preserved.contains("the_unknown"),
            "Preserve decompiler should output the_unknown format: " + preserved);
        assertTrue(preserved.contains("16") && preserved.contains("204"),
            "Preserve decompiler should include protocol and atom number: " + preserved);
    }
}

