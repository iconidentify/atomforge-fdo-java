package com.atomforge.fdo.codegen;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.text.FdoParser;
import com.atomforge.fdo.text.ast.ArgumentNode;
import com.atomforge.fdo.text.ast.ArgumentNode.*;
import com.atomforge.fdo.text.ast.AtomNode;
import com.atomforge.fdo.text.ast.StreamNode;

import java.util.*;

/**
 * Generates Java DSL source code from FDO text source.
 *
 * The generated code compiles to byte-identical binary output when executed.
 * This is achieved by emitting arguments as strings that DslTextEmitter
 * passes through unchanged to FdoCompiler.
 *
 * <p>Example input:
 * <pre>
 * uni_start_stream &lt;00x&gt;
 * man_start_object &lt;ind_group, "Window"&gt;
 * mat_orientation &lt;vcf&gt;
 * man_end_object
 * uni_end_stream
 * </pre>
 *
 * <p>Example output:
 * <pre>
 * FdoScript.stream()
 *     .atom(UniAtom.START_STREAM, "00x")
 *     .atom(ManAtom.START_OBJECT, "ind_group", "Window")
 *     .atom(MatAtom.ORIENTATION, "vcf")
 *     .atom(ManAtom.END_OBJECT)
 *     .atom(UniAtom.END_STREAM)
 *     .compile()
 * </pre>
 */
public final class DslCodeGenerator {

    private final AtomTable atomTable;

    /**
     * Create a new code generator.
     */
    public DslCodeGenerator() {
        this.atomTable = AtomTable.loadDefault();
    }

    /**
     * Generate a full Java class from FDO source.
     *
     * @param fdoSource The FDO text source
     * @param config Configuration options
     * @return Complete Java source file content
     */
    public String generate(String fdoSource, CodeGenConfig config) throws FdoException {
        StreamNode ast = FdoParser.parse(fdoSource, atomTable);
        Set<String> usedPrefixes = collectPrefixes(ast);

        StringBuilder sb = new StringBuilder();

        // Package declaration
        if (config.packageName() != null && !config.packageName().isEmpty()) {
            sb.append("package ").append(config.packageName()).append(";\n\n");
        }

        // Imports
        if (config.includeImports()) {
            sb.append("import com.atomforge.fdo.FdoException;\n");
            sb.append("import com.atomforge.fdo.dsl.FdoScript;\n");
            for (String prefix : usedPrefixes.stream().sorted().toList()) {
                String importStmt = AtomEnumMapper.getImportStatement(prefix);
                if (importStmt != null) {
                    sb.append(importStmt).append("\n");
                }
            }
            // Value enum imports for typed arguments
            Set<String> valueImports = collectValueImports(ast);
            for (String valueImport : valueImports.stream().sorted().toList()) {
                sb.append(valueImport).append("\n");
            }
            sb.append("\n");
        }

        // Class declaration
        sb.append("public final class ").append(config.className()).append(" {\n\n");

        // Private constructor
        sb.append(config.indent()).append("private ").append(config.className()).append("() {}\n\n");

        // Build method
        sb.append(config.indent()).append("public static byte[] build() throws FdoException {\n");
        sb.append(config.indent()).append(config.indent()).append("return ");
        appendBuilderChain(ast, sb, config.indent() + config.indent() + config.indent(), config);
        sb.append(";\n");
        sb.append(config.indent()).append("}\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generate a full Java class with default configuration.
     *
     * @param fdoSource The FDO text source
     * @return Complete Java source file content
     */
    public String generate(String fdoSource) throws FdoException {
        return generate(fdoSource, CodeGenConfig.fullClass("com.example.fdo", "GeneratedFdo"));
    }

    /**
     * Generate just the builder chain (method body) without class wrapper.
     *
     * @param fdoSource The FDO text source
     * @return Builder chain code (e.g., "FdoScript.stream().atom(...).compile()")
     */
    public String generateMethodBody(String fdoSource) throws FdoException {
        return generateMethodBody(fdoSource, CodeGenConfig.methodBodyOnly());
    }

    /**
     * Generate just the builder chain with configuration.
     *
     * @param fdoSource The FDO text source
     * @param config Configuration options
     * @return Builder chain code
     */
    public String generateMethodBody(String fdoSource, CodeGenConfig config) throws FdoException {
        StreamNode ast = FdoParser.parse(fdoSource, atomTable);
        StringBuilder sb = new StringBuilder();
        appendBuilderChain(ast, sb, config.indent(), config);
        return sb.toString();
    }

    /**
     * Collect all protocol prefixes used in the AST.
     */
    private Set<String> collectPrefixes(StreamNode stream) {
        Set<String> prefixes = new HashSet<>();
        for (AtomNode atom : stream.atoms()) {
            String prefix = AtomEnumMapper.extractPrefix(atom.name());
            if (prefix != null) {
                prefixes.add(prefix);
            }
            // Check nested streams in arguments
            for (ArgumentNode arg : atom.arguments()) {
                if (arg instanceof NestedStreamArg nsa) {
                    prefixes.addAll(collectPrefixes(nsa.stream()));
                }
            }
        }
        return prefixes;
    }

    /**
     * Collect all value enum imports needed for typed arguments.
     */
    private Set<String> collectValueImports(StreamNode stream) {
        Set<String> imports = new HashSet<>();
        collectValueImportsRecursive(stream, imports);
        return imports;
    }

    private void collectValueImportsRecursive(StreamNode stream, Set<String> imports) {
        for (AtomNode atom : stream.atoms()) {
            imports.addAll(TypedArgumentMapper.getImportsForAtom(atom.name()));
            // Check nested streams
            for (ArgumentNode arg : atom.arguments()) {
                if (arg instanceof NestedStreamArg nsa) {
                    collectValueImportsRecursive(nsa.stream(), imports);
                }
            }
        }
    }

    /**
     * Append the builder chain for a stream.
     */
    private void appendBuilderChain(StreamNode stream, StringBuilder sb, String indent, CodeGenConfig config) {
        if (config.useScopedOutput()) {
            appendScopedBuilderChain(stream, sb, indent, config);
            return;
        }
        sb.append("FdoScript.stream()");

        for (AtomNode atom : stream.atoms()) {
            List<ArgumentNode> args = atom.arguments();
            
            // Check if typed method exists
            String typedMethodName = TypedMethodMapper.getTypedMethodName(atom.name(), args);
            
            if (typedMethodName != null) {
                // Use typed method call
                sb.append("\n").append(indent).append(".").append(typedMethodName).append("(");
                
                // Special handling for uni_use_last_atom_string/value - they take atom references, not nested streams
                // The parser incorrectly parses atom names as nested streams, so we need to convert them back
                boolean isAtomRefAtom = atom.name().equals("uni_use_last_atom_string") || 
                                       atom.name().equals("uni_use_last_atom_value");
                
                // Separate regular args from nested streams
                NestedStreamArg nestedStream = null;
                List<ArgumentNode> regularArgs = new ArrayList<>();
                List<ArgumentNode> trailingArgs = new ArrayList<>();

                for (ArgumentNode arg : args) {
                    if (arg instanceof NestedStreamArg nsa) {
                        // For atom reference atoms, check if nested stream is actually a single atom reference
                        if (isAtomRefAtom && nsa.stream().atoms().size() == 1 && nsa.trailingData().isEmpty()) {
                            // Convert single-atom nested stream to identifier argument
                            AtomNode singleAtom = nsa.stream().atoms().get(0);
                            regularArgs.add(new ArgumentNode.IdentifierArg(
                                singleAtom.name(), singleAtom.line(), singleAtom.column()));
                        } else {
                            nestedStream = nsa;
                            trailingArgs.addAll(nsa.trailingData());
                        }
                    } else {
                        regularArgs.add(arg);
                    }
                }

                // Check if we should skip arguments (e.g., matBool methods with "yes")
                boolean skipArgs = TypedMethodMapper.shouldSkipArguments(atom.name(), args);

                // Emit regular arguments with typed argument support
                if (!skipArgs) {
                    for (int i = 0; i < regularArgs.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendArgument(regularArgs.get(i), atom.name(), i, sb, indent + config.indent(), config);
                    }
                }

                // Emit nested stream as lambda
                if (nestedStream != null) {
                    if (!skipArgs && !regularArgs.isEmpty()) sb.append(", ");
                    sb.append("nested -> {\n");
                    String nestedIndent = indent + config.indent();
                    appendNestedBuilderChain(nestedStream.stream(), sb, nestedIndent, config);
                    sb.append(indent).append("}");

                    // Emit trailing data after lambda
                    for (ArgumentNode trailing : trailingArgs) {
                        sb.append(", ");
                        appendArgument(trailing, null, -1, sb, indent, config);
                    }
                }
                
                sb.append(")");
            } else {
                // Fall back to generic .atom() call
                sb.append("\n").append(indent).append(".atom(");

                // Atom enum reference
                String enumRef = getEnumRef(atom.name());
                sb.append(enumRef);

                // Arguments
                if (!args.isEmpty()) {
                    // Check if any argument is a nested stream
                    NestedStreamArg nestedStream = null;
                    List<ArgumentNode> regularArgs = new ArrayList<>();
                    List<ArgumentNode> trailingArgs = new ArrayList<>();

                    for (ArgumentNode arg : args) {
                        if (arg instanceof NestedStreamArg nsa) {
                            nestedStream = nsa;
                            trailingArgs.addAll(nsa.trailingData());
                        } else {
                            regularArgs.add(arg);
                        }
                    }

                    // Emit regular arguments with typed argument support
                    for (int i = 0; i < regularArgs.size(); i++) {
                        sb.append(", ");
                        appendArgument(regularArgs.get(i), atom.name(), i, sb, indent + config.indent(), config);
                    }

                    // Emit nested stream as lambda
                    if (nestedStream != null) {
                        sb.append(", nested -> {\n");
                        String nestedIndent = indent + config.indent();
                        appendNestedBuilderChain(nestedStream.stream(), sb, nestedIndent, config);
                        sb.append(indent).append("}");

                        // Emit trailing data after lambda (trailing args don't get typed treatment)
                        for (ArgumentNode trailing : trailingArgs) {
                            sb.append(", ");
                            appendArgument(trailing, null, -1, sb, indent, config);
                        }
                    }
                }

                sb.append(")");
            }
        }

        sb.append("\n").append(indent.substring(config.indent().length())).append(".compile()");
    }

    /**
     * Append scoped builder chain using lambda-based object/stream methods.
     */
    private void appendScopedBuilderChain(StreamNode stream, StringBuilder sb, String indent, CodeGenConfig config) {
        sb.append("FdoScript.stream()");
        List<AtomNode> atoms = stream.atoms();
        int[] index = {0}; // Use array to allow modification in recursive calls
        appendScopedAtoms(atoms, index, sb, indent, null, config, false, false);
        sb.append("\n").append(indent.substring(config.indent().length())).append(".compile()");
    }

    /** Return value for appendScopedAtoms indicating how it terminated */
    private enum ScopeEndReason {
        /** Ended by explicit man_end_object */
        EXPLICIT_END,
        /** Ended by uni_end_stream (object has no explicit man_end_object) */
        STREAM_END,
        /** Ended by implicit man_start_sibling (sibling follows at same level) */
        SIBLING_FOLLOWS,
        /** Ended by reaching end of atom list */
        END_OF_LIST
    }

    /**
     * Append atoms in scoped format, handling nesting via recursion.
     *
     * @param atoms The list of atoms
     * @param index Current index (modified during processing)
     * @param sb Output buffer
     * @param indent Current indentation
     * @param builderVar Current builder variable name (null for top-level)
     * @param config Code generation config
     * @param inLambda Whether we're inside a lambda body (need semicolons)
     * @param inObjectScope Whether we're inside an object's lambda (for sibling handling)
     * @return Why processing stopped (affects sibling loop continuation)
     */
    private ScopeEndReason appendScopedAtoms(List<AtomNode> atoms, int[] index, StringBuilder sb,
                                    String indent, String builderVar, CodeGenConfig config,
                                    boolean inLambda, boolean inObjectScope) {
        while (index[0] < atoms.size()) {
            AtomNode atom = atoms.get(index[0]);
            String atomName = atom.name();

            // Check for scope-ending atoms
            if (atomName.equals("man_end_object")) {
                index[0]++;
                return ScopeEndReason.EXPLICIT_END;
            }
            if (atomName.equals("uni_end_stream")) {
                index[0]++;
                return ScopeEndReason.STREAM_END;
            }

            // man_start_sibling inside an object scope ends this object implicitly
            // Return WITHOUT consuming it so the parent can process it
            if (atomName.equals("man_start_sibling") && inObjectScope) {
                return ScopeEndReason.SIBLING_FOLLOWS;
            }

            // Check for scope-starting atoms
            if (atomName.equals("uni_start_stream")) {
                appendScopedStream(atoms, index, sb, indent, builderVar, config, inLambda);
            } else if (atomName.equals("man_start_object")) {
                appendScopedObject(atoms, index, sb, indent, builderVar, config, inLambda, false);
            } else if (atomName.equals("man_start_sibling")) {
                // At top-level, treat sibling like an object (shouldn't normally happen)
                appendScopedObject(atoms, index, sb, indent, builderVar, config, inLambda, true);
            } else {
                // Regular atom - emit as method call
                appendScopedAtom(atom, sb, indent, builderVar, config, inLambda);
                index[0]++;
            }
        }
        return ScopeEndReason.END_OF_LIST;
    }

    /**
     * Append a scoped stream block: stream(id, s -> { ... })
     */
    private void appendScopedStream(List<AtomNode> atoms, int[] index, StringBuilder sb,
                                     String indent, String builderVar, CodeGenConfig config, boolean inLambda) {
        AtomNode startAtom = atoms.get(index[0]);
        List<ArgumentNode> args = startAtom.arguments();
        index[0]++;

        String streamId = null;
        if (!args.isEmpty() && args.get(0) instanceof StringArg sa) {
            streamId = sa.value();
        } else if (!args.isEmpty() && args.get(0) instanceof HexArg ha) {
            streamId = ha.value();
        }

        String prefix = builderVar != null ? builderVar + "." : "\n" + indent + ".";
        String lambdaVar = "s";

        if (streamId != null && !streamId.isEmpty()) {
            sb.append(prefix).append("stream(").append(escapeString(streamId)).append(", ").append(lambdaVar).append(" -> {");
        } else {
            sb.append(prefix).append("stream(").append(lambdaVar).append(" -> {");
        }

        String innerIndent = indent + config.indent();
        sb.append("\n");
        appendScopedAtoms(atoms, index, sb, innerIndent, lambdaVar, config, true, false);
        sb.append(indent).append("})");
        if (inLambda) sb.append(";");
    }

    /**
     * Check if an object starting at startIndex has an explicit man_end_object.
     * Scans forward through the atom list, tracking nesting depth.
     *
     * @param atoms The atom list
     * @param startIndex Index of the man_start_object atom
     * @return true if object has explicit man_end_object, false if closed by uni_end_stream
     */
    private boolean hasExplicitEndObject(List<AtomNode> atoms, int startIndex) {
        int depth = 1; // Start at 1 because we're inside the object that just started
        for (int i = startIndex + 1; i < atoms.size(); i++) {
            String name = atoms.get(i).name();
            if (name.equals("man_start_object")) {
                depth++;
            } else if (name.equals("man_start_sibling")) {
                // Sibling implicitly closes previous at same depth, then opens new one
                // Net effect: depth stays the same
            } else if (name.equals("man_end_object")) {
                depth--;
                if (depth == 0) return true;  // Found explicit end for our object
            } else if (name.equals("uni_end_stream")) {
                // Stream ends before our object is explicitly closed
                return false;
            }
        }
        return false;  // End of list without explicit end
    }

    /**
     * Append a scoped object/sibling block: object(type, title, obj -> { ... })
     * After processing one object, checks for and handles any following siblings.
     *
     * If the object doesn't have an explicit man_end_object (closed by uni_end_stream),
     * uses flat atom calls instead to avoid emitting extra man_end_object.
     */
    private void appendScopedObject(List<AtomNode> atoms, int[] index, StringBuilder sb,
                                     String indent, String builderVar, CodeGenConfig config,
                                     boolean inLambda, boolean isSibling) {
        AtomNode startAtom = atoms.get(index[0]);
        List<ArgumentNode> args = startAtom.arguments();

        // Check if this object has an explicit man_end_object
        boolean hasExplicitEnd = hasExplicitEndObject(atoms, index[0]);

        // If no explicit end, use flat atom calls to avoid extra man_end_object
        if (!hasExplicitEnd && !isSibling) {
            appendFlatObject(atoms, index, sb, indent, builderVar, config, inLambda);
            return;
        }

        index[0]++;

        String methodName = isSibling ? "sibling" : "object";
        String objectType = null;
        String title = "";

        // Extract object type and title from arguments
        if (!args.isEmpty()) {
            ArgumentNode firstArg = args.get(0);
            if (firstArg instanceof ObjectTypeArg ota) {
                objectType = formatObjectType(ota.objectType());
                title = ota.title() != null ? ota.title() : "";
            } else if (firstArg instanceof IdentifierArg ia) {
                objectType = formatObjectType(ia.value());
                if (args.size() > 1 && args.get(1) instanceof StringArg sa) {
                    title = sa.value();
                }
            }
        }

        // Generate unique variable name based on object type
        String lambdaVar = generateLambdaVar(objectType, indent, config);

        String prefix = builderVar != null ? (inLambda ? indent + builderVar + "." : builderVar + ".") : "\n" + indent + ".";
        if (inLambda && builderVar != null) {
            sb.append(prefix);
        } else if (builderVar == null) {
            sb.append(prefix);
        } else {
            sb.append("\n").append(indent).append(builderVar).append(".");
        }

        sb.append(methodName).append("(");
        if (objectType != null) {
            sb.append(objectType).append(", ");
        }
        sb.append(escapeString(title)).append(", ").append(lambdaVar).append(" -> {");

        String innerIndent = indent + config.indent();
        sb.append("\n");
        ScopeEndReason endReason = appendScopedAtoms(atoms, index, sb, innerIndent, lambdaVar, config, true, true);
        sb.append(indent).append("})");
        if (inLambda) sb.append(";");
        sb.append("\n");

        // After this object closes, check if there's a sibling to process at the same level
        // Only continue if this object ended implicitly (via sibling), not explicitly (via man_end_object)
        // After explicit end, any following sibling is at the PARENT level, not this level
        while (endReason == ScopeEndReason.SIBLING_FOLLOWS && index[0] < atoms.size()) {
            AtomNode nextAtom = atoms.get(index[0]);
            if (nextAtom.name().equals("man_start_sibling")) {
                // Process the sibling at the same level (using builderVar, not lambdaVar)
                endReason = appendScopedObjectWithReason(atoms, index, sb, indent, builderVar, config, inLambda, true);
            } else {
                break; // No more siblings at this level
            }
        }
    }

    /**
     * Append an object using flat atom calls (no lambda wrapper).
     * Used for objects that don't have explicit man_end_object.
     */
    private void appendFlatObject(List<AtomNode> atoms, int[] index, StringBuilder sb,
                                   String indent, String builderVar, CodeGenConfig config,
                                   boolean inLambda) {
        AtomNode startAtom = atoms.get(index[0]);
        index[0]++;

        // Emit man_start_object as flat atom call
        appendScopedAtom(startAtom, sb, indent, builderVar, config, inLambda);

        // Process content atoms until uni_end_stream
        while (index[0] < atoms.size()) {
            AtomNode atom = atoms.get(index[0]);
            String atomName = atom.name();

            if (atomName.equals("uni_end_stream")) {
                // Don't consume uni_end_stream - let parent handle it
                break;
            }

            if (atomName.equals("man_start_object")) {
                appendScopedObject(atoms, index, sb, indent, builderVar, config, inLambda, false);
            } else if (atomName.equals("man_start_sibling")) {
                appendScopedObject(atoms, index, sb, indent, builderVar, config, inLambda, true);
            } else if (atomName.equals("man_end_object")) {
                // This is for a nested object, not our flat object
                index[0]++;
            } else {
                appendScopedAtom(atom, sb, indent, builderVar, config, inLambda);
                index[0]++;
            }
        }
    }

    /**
     * Append a scoped object and return how it ended.
     * Used by the sibling loop to determine if more siblings follow.
     */
    private ScopeEndReason appendScopedObjectWithReason(List<AtomNode> atoms, int[] index, StringBuilder sb,
                                     String indent, String builderVar, CodeGenConfig config,
                                     boolean inLambda, boolean isSibling) {
        AtomNode startAtom = atoms.get(index[0]);
        List<ArgumentNode> args = startAtom.arguments();
        index[0]++;

        String methodName = isSibling ? "sibling" : "object";
        String objectType = null;
        String title = "";

        // Extract object type and title from arguments
        if (!args.isEmpty()) {
            ArgumentNode firstArg = args.get(0);
            if (firstArg instanceof ObjectTypeArg ota) {
                objectType = formatObjectType(ota.objectType());
                title = ota.title() != null ? ota.title() : "";
            } else if (firstArg instanceof IdentifierArg ia) {
                objectType = formatObjectType(ia.value());
                if (args.size() > 1 && args.get(1) instanceof StringArg sa) {
                    title = sa.value();
                }
            }
        }

        // Generate unique variable name based on object type
        String lambdaVar = generateLambdaVar(objectType, indent, config);

        String prefix = builderVar != null ? (inLambda ? indent + builderVar + "." : builderVar + ".") : "\n" + indent + ".";
        if (inLambda && builderVar != null) {
            sb.append(prefix);
        } else if (builderVar == null) {
            sb.append(prefix);
        } else {
            sb.append("\n").append(indent).append(builderVar).append(".");
        }

        sb.append(methodName).append("(");
        if (objectType != null) {
            sb.append(objectType).append(", ");
        }
        sb.append(escapeString(title)).append(", ").append(lambdaVar).append(" -> {");

        String innerIndent = indent + config.indent();
        sb.append("\n");
        ScopeEndReason endReason = appendScopedAtoms(atoms, index, sb, innerIndent, lambdaVar, config, true, true);
        sb.append(indent).append("})");
        if (inLambda) sb.append(";");
        sb.append("\n");

        return endReason;
    }

    /**
     * Append a regular (non-scope) atom as a method call.
     */
    private void appendScopedAtom(AtomNode atom, StringBuilder sb, String indent, String builderVar,
                                   CodeGenConfig config, boolean inLambda) {
        List<ArgumentNode> args = atom.arguments();
        String typedMethodName = TypedMethodMapper.getTypedMethodName(atom.name(), args);

        String prefix = inLambda ? indent + builderVar + "." : "\n" + indent + ".";
        sb.append(prefix);

        if (typedMethodName != null) {
            sb.append(typedMethodName).append("(");

            // Handle nested streams in arguments
            NestedStreamArg nestedStream = null;
            List<ArgumentNode> regularArgs = new ArrayList<>();
            for (ArgumentNode arg : args) {
                if (arg instanceof NestedStreamArg nsa) {
                    nestedStream = nsa;
                } else {
                    regularArgs.add(arg);
                }
            }

            boolean skipArgs = TypedMethodMapper.shouldSkipArguments(atom.name(), args);
            if (!skipArgs) {
                for (int i = 0; i < regularArgs.size(); i++) {
                    if (i > 0) sb.append(", ");
                    appendArgument(regularArgs.get(i), atom.name(), i, sb, indent + config.indent(), config);
                }
            }

            // Handle nested stream as lambda
            if (nestedStream != null) {
                if (!skipArgs && !regularArgs.isEmpty()) sb.append(", ");
                sb.append("action -> {\n");
                String innerIndent = indent + config.indent();
                appendScopedNestedStream(nestedStream.stream(), sb, innerIndent, "action", config);
                sb.append(indent).append("}");
            }

            sb.append(")");
        } else {
            // Fall back to generic atom() call
            sb.append("atom(").append(getEnumRef(atom.name()));
            for (int i = 0; i < args.size(); i++) {
                sb.append(", ");
                appendArgument(args.get(i), atom.name(), i, sb, indent + config.indent(), config);
            }
            sb.append(")");
        }

        if (inLambda) sb.append(";");
        if (inLambda) sb.append("\n");
    }

    /**
     * Append nested stream content in scoped format.
     */
    private void appendScopedNestedStream(StreamNode stream, StringBuilder sb, String indent,
                                           String builderVar, CodeGenConfig config) {
        for (AtomNode atom : stream.atoms()) {
            List<ArgumentNode> args = atom.arguments();
            String typedMethodName = TypedMethodMapper.getTypedMethodName(atom.name(), args);

            sb.append(indent).append(builderVar).append(".");

            if (typedMethodName != null) {
                sb.append(typedMethodName).append("(");
                boolean skipArgs = TypedMethodMapper.shouldSkipArguments(atom.name(), args);
                if (!skipArgs) {
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendArgument(args.get(i), atom.name(), i, sb, indent + config.indent(), config);
                    }
                }
                sb.append(");\n");
            } else {
                sb.append("atom(").append(getEnumRef(atom.name()));
                for (int i = 0; i < args.size(); i++) {
                    sb.append(", ");
                    appendArgument(args.get(i), atom.name(), i, sb, indent + config.indent(), config);
                }
                sb.append(");\n");
            }
        }
    }

    /**
     * Format object type as enum reference.
     */
    private String formatObjectType(String objectType) {
        if (objectType == null) return null;
        // Try to map to ObjectType enum
        try {
            String upper = objectType.toUpperCase().replace(" ", "_");
            return "ObjectType." + upper;
        } catch (Exception e) {
            return escapeString(objectType);
        }
    }

    /**
     * Generate a lambda variable name based on object type and nesting depth.
     * Uses depth to ensure unique variable names at each nesting level.
     */
    private String generateLambdaVar(String objectType, String indent, CodeGenConfig config) {
        if (objectType == null) return "obj";
        String type = objectType.replace("ObjectType.", "").toLowerCase();

        // Calculate depth from indent (each level is one indent)
        int depth = indent.length() / config.indent().length();

        String baseName = switch (type) {
            case "ind_group" -> "root";
            case "org_group" -> "grp";
            case "view" -> "view";
            case "edit_view" -> "edit";
            case "trigger" -> "btn";
            case "menu_item" -> "item";
            case "list" -> "list";
            case "dms_list" -> "list";
            case "notebook" -> "nb";
            case "tab" -> "tab";
            default -> "obj";
        };

        // Append depth suffix for org_group (most common duplicate) and obj at depth > 1
        if ((type.equals("org_group") || baseName.equals("obj")) && depth > 1) {
            return baseName + depth;
        }
        return baseName;
    }

    /**
     * Append builder chain for nested stream (without FdoScript.stream() wrapper).
     */
    private void appendNestedBuilderChain(StreamNode stream, StringBuilder sb, String indent, CodeGenConfig config) {
        for (AtomNode atom : stream.atoms()) {
            List<ArgumentNode> args = atom.arguments();
            
            // Check if typed method exists
            String typedMethodName = TypedMethodMapper.getTypedMethodName(atom.name(), args);
            
            if (typedMethodName != null) {
                // Use typed method call
                sb.append(indent).append("nested.").append(typedMethodName).append("(");
                
                // Separate regular args from nested streams
                NestedStreamArg innerNested = null;
                List<ArgumentNode> regularArgs = new ArrayList<>();
                List<ArgumentNode> trailingArgs = new ArrayList<>();

                for (ArgumentNode arg : args) {
                    if (arg instanceof NestedStreamArg nsa) {
                        innerNested = nsa;
                        trailingArgs.addAll(nsa.trailingData());
                    } else {
                        regularArgs.add(arg);
                    }
                }

                // Check if we should skip arguments (e.g., matBool methods with "yes")
                boolean skipArgs = TypedMethodMapper.shouldSkipArguments(atom.name(), args);

                if (!skipArgs) {
                    for (int i = 0; i < regularArgs.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendArgument(regularArgs.get(i), atom.name(), i, sb, indent + config.indent(), config);
                    }
                }

                if (innerNested != null) {
                    if (!skipArgs && !regularArgs.isEmpty()) sb.append(", ");
                    sb.append("inner -> {\n");
                    String innerIndent = indent + config.indent();
                    appendDeepNestedBuilderChain(innerNested.stream(), sb, innerIndent, "inner", config);
                    sb.append(indent).append("}");

                    for (ArgumentNode trailing : trailingArgs) {
                        sb.append(", ");
                        appendArgument(trailing, null, -1, sb, indent, config);
                    }
                }

                sb.append(");\n");
            } else {
                // Fall back to generic .atom() call
                sb.append(indent).append("nested.atom(");

                String enumRef = getEnumRef(atom.name());
                sb.append(enumRef);

                if (!args.isEmpty()) {
                    // Check for nested streams within nested streams
                    NestedStreamArg innerNested = null;
                    List<ArgumentNode> regularArgs = new ArrayList<>();
                    List<ArgumentNode> trailingArgs = new ArrayList<>();

                    for (ArgumentNode arg : args) {
                        if (arg instanceof NestedStreamArg nsa) {
                            innerNested = nsa;
                            trailingArgs.addAll(nsa.trailingData());
                        } else {
                            regularArgs.add(arg);
                        }
                    }

                    for (int i = 0; i < regularArgs.size(); i++) {
                        sb.append(", ");
                        appendArgument(regularArgs.get(i), atom.name(), i, sb, indent + config.indent(), config);
                    }

                    if (innerNested != null) {
                        sb.append(", inner -> {\n");
                        String innerIndent = indent + config.indent();
                        appendDeepNestedBuilderChain(innerNested.stream(), sb, innerIndent, "inner", config);
                        sb.append(indent).append("}");

                        for (ArgumentNode trailing : trailingArgs) {
                            sb.append(", ");
                            appendArgument(trailing, null, -1, sb, indent, config);
                        }
                    }
                }

                sb.append(");\n");
            }
        }
    }

    /**
     * Append deeply nested builder chain with custom builder name.
     */
    private void appendDeepNestedBuilderChain(StreamNode stream, StringBuilder sb, String indent,
                                               String builderName, CodeGenConfig config) {
        for (AtomNode atom : stream.atoms()) {
            List<ArgumentNode> args = atom.arguments();
            
            // Check if typed method exists
            String typedMethodName = TypedMethodMapper.getTypedMethodName(atom.name(), args);
            
            if (typedMethodName != null) {
                // Use typed method call
                sb.append(indent).append(builderName).append(".").append(typedMethodName).append("(");
                
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) sb.append(", ");
                    appendArgument(args.get(i), atom.name(), i, sb, indent + config.indent(), config);
                }
                
                sb.append(");\n");
            } else {
                // Fall back to generic .atom() call
                sb.append(indent).append(builderName).append(".atom(");

                String enumRef = getEnumRef(atom.name());
                sb.append(enumRef);

                for (int i = 0; i < args.size(); i++) {
                    sb.append(", ");
                    appendArgument(args.get(i), atom.name(), i, sb, indent + config.indent(), config);
                }

                sb.append(");\n");
            }
        }
    }

    /**
     * Append a single argument with optional typed enum support.
     *
     * @param arg The argument node
     * @param atomName The atom name (null to skip typed lookup)
     * @param argIndex The argument index (0-based)
     * @param sb The output buffer
     * @param indent Current indentation
     * @param config Code generation config
     */
    private void appendArgument(ArgumentNode arg, String atomName, int argIndex,
                                StringBuilder sb, String indent, CodeGenConfig config) {
        switch (arg) {
            case StringArg sa -> sb.append(escapeString(sa.value()));
            case NumberArg na -> {
                // Try to emit as typed enum if applicable (e.g., FontId, FontStyle)
                String typedRef = atomName != null
                    ? TypedArgumentMapper.tryFormatNumericArg(atomName, argIndex, na.value())
                    : null;
                if (typedRef != null) {
                    sb.append(typedRef);
                } else {
                    sb.append(na.value());
                }
            }
            case HexArg ha -> sb.append(escapeString(ha.value()));
            case GidArg ga -> {
                // Try to emit as FdoGid.of() for typed methods
                String gidRef = atomName != null
                    ? TypedArgumentMapper.tryFormatGidArg(atomName, argIndex, ga.value())
                    : null;
                if (gidRef != null) {
                    sb.append(gidRef);
                } else {
                    sb.append(escapeString(ga.value()));
                }
            }
            case IdentifierArg ia -> {
                // Try to emit as typed enum if applicable
                String typedRef = atomName != null
                    ? TypedArgumentMapper.tryFormatTypedArg(atomName, argIndex, ia.value())
                    : null;
                if (typedRef != null) {
                    sb.append(typedRef);
                } else {
                    sb.append(escapeString(ia.value()));
                }
            }
            case PipedArg pa -> {
                // Try to emit as typed enum if applicable (e.g., TitlePosition)
                String pipedStr = formatPipedArgValue(pa);
                String typedRef = atomName != null
                    ? TypedArgumentMapper.tryFormatPipedArg(atomName, argIndex, pipedStr)
                    : null;
                if (typedRef != null) {
                    sb.append(typedRef);
                } else {
                    sb.append(formatPipedArg(pa));
                }
            }
            case ListArg la -> appendListArg(la, atomName, argIndex, sb, indent, config);
            case ObjectTypeArg ota -> appendObjectTypeArg(ota, atomName, sb);
            case NestedStreamArg nsa -> {
                // Should be handled at atom level, but provide fallback
                sb.append("/* nested stream */");
            }
        }
    }

    /**
     * Append list argument - expands into multiple args.
     */
    private void appendListArg(ListArg la, String atomName, int argIndex,
                               StringBuilder sb, String indent, CodeGenConfig config) {
        List<ArgumentNode> elements = la.elements();
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(", ");
            // List elements continue from the same arg position
            appendArgument(elements.get(i), atomName, argIndex + i, sb, indent, config);
        }
    }

    /**
     * Append object type argument with typed enum support.
     */
    private void appendObjectTypeArg(ObjectTypeArg ota, String atomName, StringBuilder sb) {
        // Try to emit as typed ObjectType enum
        String typedRef = atomName != null
            ? TypedArgumentMapper.tryFormatTypedArg(atomName, 0, ota.objectType())
            : null;
        if (typedRef != null) {
            sb.append(typedRef);
        } else {
            sb.append(escapeString(ota.objectType()));
        }
        // Always include title if present (even empty string is significant in FDO)
        if (ota.title() != null) {
            sb.append(", ").append(escapeString(ota.title()));
        }
    }

    /**
     * Format piped argument as a quoted string: "left | center"
     */
    private String formatPipedArg(PipedArg pa) {
        return '"' + formatPipedArgValue(pa) + '"';
    }

    /**
     * Format piped argument value without quotes: left | center
     */
    private String formatPipedArgValue(PipedArg pa) {
        StringBuilder sb = new StringBuilder();
        List<ArgumentNode> parts = pa.parts();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(" | ");
            ArgumentNode part = parts.get(i);
            if (part instanceof IdentifierArg ia) {
                sb.append(ia.value());
            } else if (part instanceof NumberArg na) {
                sb.append(na.value());
            } else if (part instanceof StringArg sa) {
                sb.append(sa.value());
            }
        }
        return sb.toString();
    }

    /**
     * Get the enum reference for an atom name.
     * Returns DSL enum reference (e.g., "ManAtom.START_OBJECT") for known atoms,
     * or RawAtom.of() expression for unknown atoms.
     */
    private String getEnumRef(String atomName) {
        try {
            return AtomEnumMapper.mapToEnumRef(atomName);
        } catch (IllegalArgumentException e) {
            // Unknown atom - use RawAtom
            return "RawAtom.of(\"" + atomName + "\")";
        }
    }

    /**
     * Escape a string for Java source code.
     */
    private String escapeString(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
