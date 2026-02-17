package com.atomforge.fdo.codegen;

import com.atomforge.fdo.FdoCompiler;
import com.atomforge.fdo.FdoException;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify auto-generated Java DSL code produces byte-identical
 * output to direct text compilation.
 *
 * These tests use JShell to dynamically execute the generated Java code,
 * proving that the code generator produces correct, executable code that
 * compiles to identical binary output.
 */
class DslCodeGeneratorExecutionTest {

    private static DslCodeGenerator generator;
    private static FdoCompiler textCompiler;

    @BeforeAll
    static void setUp() {
        generator = new DslCodeGenerator();
        textCompiler = FdoCompiler.create();
    }

    @Test
    @DisplayName("Generated DSL from newtest.fdo.txt produces byte-identical output")
    void testNewtestFdoGeneratedDslProducesIdenticalBinary() throws FdoException, IOException {
        // Load the test FDO source
        String fdoSource = loadResource("/newtest.fdo.txt");
        assertNotNull(fdoSource, "Test resource newtest.fdo.txt not found");

        // Compile directly from text to get expected bytes
        byte[] expectedBytes = textCompiler.compile(fdoSource);

        // Generate Java DSL code with scoped output (lambda-based)
        // DslTextEmitter optimizes away redundant man_end_object before man_start_sibling
        String generatedDsl = generator.generateMethodBody(fdoSource);
        assertNotNull(generatedDsl, "Generated DSL should not be null");
        assertTrue(generatedDsl.contains("FdoScript.stream()"), "Generated DSL should contain FdoScript.stream()");
        assertTrue(generatedDsl.contains(".compile()"), "Generated DSL should end with .compile()");

        // Execute the generated DSL code using JShell
        byte[] actualBytes = executeDslWithJShell(generatedDsl);

        // Compare binary output
        assertArrayEquals(expectedBytes, actualBytes,
            () -> buildMismatchMessage(expectedBytes, actualBytes, fdoSource, generatedDsl));
    }

    @Test
    @DisplayName("Generated DSL uses typed methods for newtest.fdo.txt")
    void testNewtestFdoGeneratedDslUsesTypedMethods() throws FdoException, IOException {
        String fdoSource = loadResource("/newtest.fdo.txt");
        String generatedDsl = generator.generateMethodBody(fdoSource);

        // Verify scoped or typed methods are used (not generic .atom() calls)
        // Scoped output uses .stream() and .object() instead of .uniStartStream() and .manStartObject()
        assertTrue(generatedDsl.contains(".stream(") || generatedDsl.contains(".uniStartStream(") || generatedDsl.contains("UniAtom.START_STREAM"),
            "Should use scoped stream(), typed uniStartStream, or enum");
        assertTrue(generatedDsl.contains(".object(ObjectType.") || generatedDsl.contains(".manStartObject(") || generatedDsl.contains("ManAtom.START_OBJECT"),
            "Should use scoped object(), typed manStartObject, or enum");
        assertTrue(generatedDsl.contains(".matOrientation(") || generatedDsl.contains("MatAtom.ORIENTATION"),
            "Should use typed mat_orientation method or enum");

        // Verify typed enums are used where appropriate
        assertTrue(generatedDsl.contains("ObjectType.IND_GROUP") || generatedDsl.contains("\"ind_group\""),
            "Should reference ind_group object type");
        assertTrue(generatedDsl.contains("Orientation.") || generatedDsl.contains("\"v"),
            "Should reference orientation values");
    }

    /**
     * Execute generated DSL code using JShell and return the compiled bytes.
     */
    private byte[] executeDslWithJShell(String dslCode) {
        try (JShell shell = JShell.builder()
                .executionEngine(new LocalExecutionControlProvider(), null)
                .build()) {

            // Add required imports
            evalOrFail(shell, "import com.atomforge.fdo.dsl.FdoScript;");
            evalOrFail(shell, "import com.atomforge.fdo.dsl.StreamBuilder;");
            evalOrFail(shell, "import com.atomforge.fdo.dsl.atoms.*;");
            evalOrFail(shell, "import com.atomforge.fdo.dsl.values.*;");
            evalOrFail(shell, "import com.atomforge.fdo.dsl.internal.*;");
            evalOrFail(shell, "import com.atomforge.fdo.model.FdoGid;");

            // Build DSL without compile() to get the intermediate builder
            String dslWithoutCompile = dslCode.replace(".compile()", "");
            String builderCode = "StreamBuilder builder = " + dslWithoutCompile + ";";
            List<SnippetEvent> builderEvents = shell.eval(builderCode);
            for (SnippetEvent event : builderEvents) {
                if (event.status() == Snippet.Status.REJECTED) {
                    fail("JShell rejected builder code:\n" + builderCode + "\n\nDiagnostics: " +
                         shell.diagnostics(event.snippet()).toList());
                }
            }

            // Now compile to get bytes
            String compileCode = "byte[] result = builder.compile();";
            List<SnippetEvent> events = shell.eval(compileCode);
            for (SnippetEvent event : events) {
                if (event.status() == Snippet.Status.REJECTED) {
                    fail("JShell rejected compile code");
                }
                if (event.exception() != null) {
                    fail("JShell execution threw exception: " + event.exception().getMessage());
                }
            }

            return parseByteArrayFromJShell(shell, "result");
        }
    }

    /**
     * Parse byte array result from JShell.
     */
    private byte[] parseByteArrayFromJShell(JShell shell, String varName) {
        // Get the length first
        List<SnippetEvent> lengthEvents = shell.eval(varName + ".length");
        int length = 0;
        for (SnippetEvent event : lengthEvents) {
            if (event.value() != null) {
                length = Integer.parseInt(event.value());
                break;
            }
        }

        // Retrieve bytes one at a time (most reliable method)
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            List<SnippetEvent> byteEvents = shell.eval(varName + "[" + i + "]");
            for (SnippetEvent event : byteEvents) {
                if (event.value() != null) {
                    result[i] = Byte.parseByte(event.value());
                    break;
                }
            }
        }

        return result;
    }

    private void evalOrFail(JShell shell, String code) {
        List<SnippetEvent> events = shell.eval(code);
        for (SnippetEvent event : events) {
            if (event.status() == Snippet.Status.REJECTED) {
                fail("JShell import failed: " + code + "\nDiagnostics: " +
                     shell.diagnostics(event.snippet()).toList());
            }
        }
    }

    private String loadResource(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String buildMismatchMessage(byte[] expected, byte[] actual, String source, String dsl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generated DSL produced different binary output.\n\n");
        sb.append("Expected length: ").append(expected.length).append(" bytes\n");
        sb.append("Actual length: ").append(actual.length).append(" bytes\n\n");

        // Find first difference
        int diffIndex = -1;
        for (int i = 0; i < Math.min(expected.length, actual.length); i++) {
            if (expected[i] != actual[i]) {
                diffIndex = i;
                break;
            }
        }
        if (diffIndex == -1 && expected.length != actual.length) {
            diffIndex = Math.min(expected.length, actual.length);
        }

        if (diffIndex >= 0) {
            sb.append("First difference at byte ").append(diffIndex).append(":\n");
            sb.append("  Expected: ").append(formatByte(expected, diffIndex)).append("\n");
            sb.append("  Actual: ").append(formatByte(actual, diffIndex)).append("\n\n");
        }

        sb.append("Source excerpt (first 500 chars):\n");
        sb.append(source.substring(0, Math.min(500, source.length()))).append("\n\n");

        sb.append("Generated DSL excerpt (first 1000 chars):\n");
        sb.append(dsl.substring(0, Math.min(1000, dsl.length())));

        return sb.toString();
    }

    private String formatByte(byte[] arr, int index) {
        if (index >= arr.length) {
            return "(end of array)";
        }
        return String.format("0x%02X (%d)", arr[index] & 0xFF, arr[index] & 0xFF);
    }
}
