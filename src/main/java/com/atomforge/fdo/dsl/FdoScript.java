package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.model.FdoStream;

/**
 * Entry point for the type-safe FDO DSL.
 *
 * This class provides static factory methods for creating FDO scripts
 * programmatically with compile-time type safety and IDE autocomplete.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * byte[] binary = FdoScript.stream()
 *     .startObject(ObjectType.IND_GROUP, "Login")
 *         .orientation(Orientation.VCF)
 *         .position(Position.CENTER_CENTER)
 *     .endObject()
 *     .compile();
 * }</pre>
 *
 * <h2>With Actions</h2>
 * <pre>{@code
 * byte[] binary = FdoScript.stream()
 *     .startObject(ObjectType.TRIGGER, "OK")
 *         .triggerStyle(TriggerStyle.FRAMED)
 *         .onSelect(action -> action.data("Button pressed"))
 *     .endObject()
 *     .compile();
 * }</pre>
 *
 * <h2>Using Atom Enums Directly</h2>
 * <pre>{@code
 * byte[] binary = FdoScript.stream()
 *     .uni(UniAtom.START_STREAM)
 *     .man(ManAtom.START_OBJECT, ObjectType.VIEW, "Text")
 *     .mat(MatAtom.SIZE, 3, 40)
 *     .de(DeAtom.DATA, "Hello, World!")
 *     .man(ManAtom.END_OBJECT)
 *     .uni(UniAtom.END_STREAM)
 *     .compile();
 * }</pre>
 *
 * @see StreamBuilder
 * @see ObjectBuilder
 */
public final class FdoScript {

    private FdoScript() {} // Static utility class

    /**
     * Create a new stream builder.
     *
     * This is the main entry point for building FDO scripts.
     *
     * @return A new StreamBuilder for fluent stream construction
     */
    public static StreamBuilder stream() {
        return new StreamBuilder();
    }

    /**
     * Parse binary FDO data into an FdoStream.
     *
     * This provides a bridge from the DSL to the existing model API
     * for inspecting and manipulating decoded streams.
     *
     * @param binary The binary FDO data to parse
     * @return The decoded FdoStream
     * @throws FdoException if parsing fails
     */
    public static FdoStream parse(byte[] binary) throws FdoException {
        return FdoStream.decode(binary);
    }

    /**
     * Compile an FDO text source to binary.
     *
     * This provides a convenience method for compiling text-format FDO
     * using the existing compiler.
     *
     * @param source The FDO text source
     * @return The compiled binary
     * @throws FdoException if compilation fails
     */
    public static byte[] compile(String source) throws FdoException {
        return com.atomforge.fdo.FdoCompiler.create().compile(source);
    }

    /**
     * Decompile binary FDO data to text format.
     *
     * This provides a convenience method for decompiling binary FDO
     * using the existing decompiler.
     *
     * @param binary The binary FDO data
     * @return The decompiled text source
     * @throws FdoException if decompilation fails
     */
    public static String decompile(byte[] binary) throws FdoException {
        return com.atomforge.fdo.FdoDecompiler.create().decompile(binary);
    }
}
