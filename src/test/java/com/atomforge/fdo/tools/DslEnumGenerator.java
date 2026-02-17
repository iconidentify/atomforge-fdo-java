package com.atomforge.fdo.tools;

import com.atomforge.fdo.atom.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Code generator for DSL atom enums.
 *
 * Generates one enum per protocol from the AtomRegistry, each implementing DslAtom.
 *
 * <p><strong>Architecture:</strong> Generated enums delegate to AtomDefinition from
 * the AtomRegistry at runtime. This ensures a single source of truth for all atom
 * metadata (protocol, atomNumber, type, flags) and prevents bugs from duplicated data.
 *
 * <p>Usage: java DslEnumGenerator [output-directory]
 * <p>Default output: src/main/java/com/atomforge/fdo/dsl/atoms/
 */
public class DslEnumGenerator {

    private static final String PACKAGE = "com.atomforge.fdo.dsl.atoms";
    private static final String DEFAULT_OUTPUT_DIR = "src/main/java/com/atomforge/fdo/dsl/atoms";

    // Protocol number to class name mapping
    private static final Map<Integer, String> PROTOCOL_NAMES = Map.ofEntries(
        Map.entry(Protocol.UNI, "Uni"),
        Map.entry(Protocol.MAN, "Man"),
        Map.entry(Protocol.ACT, "Act"),
        Map.entry(Protocol.DE, "De"),
        Map.entry(Protocol.BUF, "Buf"),
        Map.entry(Protocol.IDB, "Idb"),
        Map.entry(Protocol.XFER, "Xfer"),
        Map.entry(Protocol.FM, "Fm"),
        Map.entry(Protocol.LM, "Lm"),
        Map.entry(Protocol.CM, "Cm"),
        Map.entry(Protocol.CHAT, "Chat"),
        Map.entry(Protocol.VAR, "Var"),
        Map.entry(Protocol.ASYNC, "Async"),
        Map.entry(Protocol.SM, "Sm"),
        Map.entry(Protocol.IF, "If"),
        Map.entry(Protocol.MAT, "Mat"),
        Map.entry(Protocol.MIP, "Mip"),
        Map.entry(Protocol.REG, "Reg"),
        Map.entry(Protocol.FONT, "Font"),
        Map.entry(Protocol.MMI, "Mmi"),
        Map.entry(Protocol.IMGXFER, "Imgxfer"),
        Map.entry(Protocol.IMAGE, "Image"),
        Map.entry(Protocol.CHART, "Chart"),
        Map.entry(Protocol.MORG, "Morg"),
        Map.entry(Protocol.RICH, "Rich"),
        Map.entry(Protocol.EXAPI, "Exapi"),
        Map.entry(Protocol.DOD, "Dod"),
        Map.entry(Protocol.RADIO, "Radio"),
        Map.entry(Protocol.PICTALK, "Pictalk"),
        Map.entry(Protocol.IRC, "Irc"),
        Map.entry(Protocol.DOC, "Doc"),
        Map.entry(Protocol.VIDEO, "Video"),
        Map.entry(Protocol.SND, "Snd"),
        Map.entry(Protocol.CCL, "Ccl"),
        Map.entry(Protocol.P3, "P3"),
        Map.entry(Protocol.STATS, "Stats"),
        Map.entry(Protocol.PT, "Pt"),
        Map.entry(Protocol.PAKMAN, "Pakman"),
        Map.entry(Protocol.AD, "Ad"),
        Map.entry(Protocol.APP, "App"),
        Map.entry(Protocol.CONTEXT, "Context"),
        Map.entry(Protocol.MT, "Mt"),
        Map.entry(Protocol.DBRES, "Dbres"),
        Map.entry(Protocol.MODEM, "Modem"),
        Map.entry(Protocol.TCP, "Tcp"),
        Map.entry(Protocol.VRM, "Vrm"),
        Map.entry(Protocol.WWW, "Www"),
        Map.entry(Protocol.AOLSOCK, "Aolsock"),
        Map.entry(Protocol.PPP, "Ppp"),
        Map.entry(Protocol.HFS, "Hfs"),
        Map.entry(Protocol.BLANK, "Blank"),
        Map.entry(Protocol.VID, "Vid"),
        Map.entry(Protocol.STARTUP, "Startup"),
        Map.entry(Protocol.FAX, "Fax")
    );

    public static void main(String[] args) throws IOException {
        String outputDir = args.length > 0 ? args[0] : DEFAULT_OUTPUT_DIR;
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        // Group atoms by protocol
        Map<Integer, List<AtomDefinition>> atomsByProtocol = AtomRegistry.getAllAtoms().stream()
            .collect(Collectors.groupingBy(AtomDefinition::protocol, TreeMap::new, Collectors.toList()));

        System.out.println("Generating DSL atom enums (delegation pattern)...");
        System.out.println("Total atoms: " + AtomRegistry.getAllAtoms().size());
        System.out.println("Protocols: " + atomsByProtocol.size());

        int totalGenerated = 0;

        for (Map.Entry<Integer, List<AtomDefinition>> entry : atomsByProtocol.entrySet()) {
            int protocol = entry.getKey();
            List<AtomDefinition> atoms = entry.getValue();

            String className = PROTOCOL_NAMES.getOrDefault(protocol, "Proto" + protocol);
            String enumName = className + "Atom";

            String code = generateEnum(protocol, enumName, atoms);
            Path file = outputPath.resolve(enumName + ".java");
            Files.writeString(file, code);

            System.out.println("  Generated " + enumName + ".java (" + atoms.size() + " atoms)");
            totalGenerated += atoms.size();
        }

        System.out.println("\nGeneration complete!");
        System.out.println("Total enums: " + atomsByProtocol.size());
        System.out.println("Total atoms: " + totalGenerated);
    }

    private static String generateEnum(int protocol, String enumName, List<AtomDefinition> atoms) {
        StringBuilder sb = new StringBuilder();
        String prefix = Protocol.getPrefix(protocol);

        // Package and imports
        sb.append("package ").append(PACKAGE).append(";\n\n");
        sb.append("import com.atomforge.fdo.atom.AtomDefinition;\n");
        sb.append("import com.atomforge.fdo.atom.AtomTable;\n\n");

        // Enum declaration
        sb.append("public enum ").append(enumName).append(" implements DslAtom {\n");

        // Enum constants - just names, no data!
        for (int i = 0; i < atoms.size(); i++) {
            AtomDefinition atom = atoms.get(i);
            String constantName = toEnumConstant(atom.name(), protocol);
            sb.append("    ").append(constantName);
            sb.append(i < atoms.size() - 1 ? ",\n" : ";\n");
        }

        // Cached definition field
        sb.append("\n    private final AtomDefinition def;\n");

        // Constructor - looks up from AtomTable
        sb.append("\n    ").append(enumName).append("() {\n");
        sb.append("        String atomName = \"").append(prefix).append("_\" + name().toLowerCase();\n");
        sb.append("        this.def = AtomTable.loadDefault().findByName(atomName)\n");
        sb.append("            .orElseThrow(() -> new IllegalStateException(\n");
        sb.append("                \"Atom not found in registry: \" + atomName + \n");
        sb.append("                \". This indicates a mismatch between DslEnumGenerator and AtomRegistry.\"));\n");
        sb.append("    }\n");

        // DslAtom interface method - the only one needed!
        sb.append("\n    @Override\n");
        sb.append("    public AtomDefinition definition() {\n");
        sb.append("        return def;\n");
        sb.append("    }\n");

        // Lookup by atom number
        sb.append("\n    public static ").append(enumName).append(" fromAtomNumber(int atomNumber) {\n");
        sb.append("        for (").append(enumName).append(" atom : values()) {\n");
        sb.append("            if (atom.atomNumber() == atomNumber) {\n");
        sb.append("                return atom;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");

        // Lookup by name
        sb.append("\n    public static ").append(enumName).append(" fromName(String name) {\n");
        sb.append("        if (name == null) return null;\n");
        sb.append("        String lower = name.toLowerCase();\n");
        sb.append("        for (").append(enumName).append(" atom : values()) {\n");
        sb.append("            if (atom.atomName().equals(lower)) {\n");
        sb.append("                return atom;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Convert atom name to Java enum constant.
     * e.g., "uni_start_stream" -> "START_STREAM"
     *       "de_data" -> "DATA"
     */
    private static String toEnumConstant(String atomName, int protocol) {
        String prefix = Protocol.getPrefix(protocol) + "_";
        String name = atomName.toLowerCase();

        // Remove protocol prefix
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }

        // Convert to UPPER_CASE
        return name.toUpperCase().replace('-', '_');
    }
}
