package com.atomforge.fdo;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.atom.AtomRegistry;
import com.atomforge.fdo.atom.AtomTable;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.tools.AdaBinParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification tests for AtomRegistry integrity.
 *
 * These tests verify that the registry is internally consistent,
 * without requiring the original ADA.BIN file.
 *
 * Note: ADA.BIN was used to generate AtomRegistry.java but is not
 * redistributed with this project due to licensing.
 */
class AdaBinVerificationTest {

    private static List<AtomDefinition> registryAtoms;
    private static AtomTable atomTable;

    @BeforeAll
    static void setUp() {
        registryAtoms = AtomRegistry.getAllAtoms();
        atomTable = AtomTable.loadDefault();
    }

    @Test
    void verifyExpectedAtomCount() {
        // The registry was generated from ADA.BIN with 1,628 atoms
        assertThat(registryAtoms.size())
            .as("Registry should have 1,887 atoms")
            .isEqualTo(1887);
    }

    @Test
    void verifyNoDuplicateNames() {
        Set<String> names = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (AtomDefinition atom : registryAtoms) {
            if (!names.add(atom.name())) {
                duplicates.add(atom.name());
            }
        }

        assertThat(duplicates)
            .as("No duplicate atom names should exist")
            .isEmpty();
    }

    @Test
    void verifyNoDuplicateKeys() {
        // Check no duplicate protocol+atom combinations
        Set<Long> keys = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (AtomDefinition atom : registryAtoms) {
            long key = ((long) atom.protocol() << 16) | atom.atomNumber();
            if (!keys.add(key)) {
                duplicates.add(String.format("%s (proto=%d, atom=%d)",
                    atom.name(), atom.protocol(), atom.atomNumber()));
            }
        }

        assertThat(duplicates)
            .as("No duplicate protocol+atom combinations should exist")
            .isEmpty();
    }

    @Test
    void verifyAllAtomsHaveValidTypes() {
        for (AtomDefinition atom : registryAtoms) {
            assertThat(atom.type())
                .as("Atom %s should have a valid type", atom.name())
                .isNotNull();
        }
    }

    @Test
    void verifyCriticalAtomsExist() {
        // These atoms are essential for basic FDO processing
        String[] criticalAtoms = {
            "uni_void",
            "uni_start_stream",
            "uni_end_stream",
            "uni_abort_stream",
            "man_start_object",
            "man_end_object",
            "mat_object_id",
            "mat_orientation",
            "act_set_criterion",
            "act_do_action"
        };

        for (String name : criticalAtoms) {
            assertThat(atomTable.findByName(name))
                .as("Critical atom '%s' must exist in registry", name)
                .isPresent();
        }
    }

    @Test
    void verifyCriticalAtomNumbers() {
        // Verify specific atom numbers that are hardcoded in protocols
        assertThat(atomTable.findByName("uni_start_stream"))
            .isPresent()
            .get()
            .satisfies(atom -> {
                assertThat(atom.protocol()).isEqualTo(0);
                assertThat(atom.atomNumber()).isEqualTo(1);
            });

        assertThat(atomTable.findByName("uni_end_stream"))
            .isPresent()
            .get()
            .satisfies(atom -> {
                assertThat(atom.protocol()).isEqualTo(0);
                assertThat(atom.atomNumber()).isEqualTo(2);
            });

        assertThat(atomTable.findByName("man_start_object"))
            .isPresent()
            .get()
            .satisfies(atom -> {
                assertThat(atom.protocol()).isEqualTo(1);
                assertThat(atom.atomNumber()).isEqualTo(0);
            });
    }

    @Test
    void verifyAtomTableLookupByKey() {
        // Test that atoms can be found by protocol+atom number
        Optional<AtomDefinition> found = atomTable.findByProtocolAtom(0, 1);
        assertThat(found)
            .isPresent()
            .get()
            .satisfies(atom -> assertThat(atom.name()).isEqualTo("uni_start_stream"));
    }

    @Test
    void verifyAdaBinParserWithSampleData() {
        // Test the parser with a small in-memory sample that mimics ADA.BIN format
        // This verifies the parser logic works without needing the actual file
        //
        // ADA.BIN format: [ffed atom#] [ffed type] [ffed flags] ... [ffed name_len] <name>
        // Marker 0xFFED followed by 16-bit big-endian value

        byte[] sampleData = new byte[] {
            // Padding/header (simulated)
            0x00, 0x01,
            // Atom entry pattern: [ffed 0001] [ffed 0001] [ffed 0001] ... [ffed 0010] "uni_start_stream"
            (byte)0xFF, (byte)0xED, 0x00, 0x01,  // atom number = 1
            (byte)0xFF, (byte)0xED, 0x00, 0x01,  // type code = RAW
            (byte)0xFF, (byte)0xED, 0x00, 0x01,  // flags = INDENT
            (byte)0xFF, (byte)0xEA, 0x00, 0x00, 0x00, 0x00,  // pointer (ignored)
            (byte)0xFF, (byte)0xED, 0x00, 0x10,  // name length = 16
            // "uni_start_stream"
            'u', 'n', 'i', '_', 's', 't', 'a', 'r', 't', '_', 's', 't', 'r', 'e', 'a', 'm',
        };

        AdaBinParser parser = new AdaBinParser();
        List<AtomDefinition> parsed = parser.parse(sampleData);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).name()).isEqualTo("uni_start_stream");
        assertThat(parsed.get(0).atomNumber()).isEqualTo(1);
        assertThat(parsed.get(0).type()).isEqualTo(AtomType.RAW);
        assertThat(parsed.get(0).flags()).contains(AtomFlags.INDENT);
    }

    @Test
    void printRegistryStatistics() {
        System.out.println("\n=== AtomRegistry Statistics ===");
        System.out.println("Total atoms: " + registryAtoms.size());

        // Count by protocol
        Map<Integer, Long> byProtocol = registryAtoms.stream()
            .collect(Collectors.groupingBy(AtomDefinition::protocol, Collectors.counting()));

        System.out.println("\nAtoms by protocol:");
        byProtocol.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(10)
            .forEach(e -> System.out.printf("  Protocol %2d: %d atoms%n", e.getKey(), e.getValue()));
        System.out.println("  ... and " + (byProtocol.size() - 10) + " more protocols");

        // Count by type
        Map<AtomType, Long> byType = registryAtoms.stream()
            .collect(Collectors.groupingBy(AtomDefinition::type, Collectors.counting()));

        System.out.println("\nTop 5 atom types:");
        byType.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(5)
            .forEach(e -> System.out.printf("  %s: %d atoms%n", e.getKey(), e.getValue()));
    }
}
