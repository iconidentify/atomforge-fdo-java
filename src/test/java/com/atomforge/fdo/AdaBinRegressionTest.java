package com.atomforge.fdo;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.atom.AtomRegistry;
import com.atomforge.fdo.atom.AtomType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Regression tests to ensure AtomRegistry changes don't break existing functionality.
 *
 * This test compares the current AtomRegistry against a known-good baseline snapshot.
 * If a future ADA.BIN import changes the registry, these tests will catch:
 * - Removed atoms (would break existing code)
 * - Changed atom numbers (would break binary compatibility)
 * - Changed protocol numbers (would break wire protocol)
 * - Changed type codes (might break encoding/decoding)
 */
class AdaBinRegressionTest {

    private static List<AtomDefinition> registryAtoms;
    private static List<BaselineAtom> baselineAtoms;
    private static Map<String, BaselineAtom> baselineByName;
    private static boolean baselineAvailable = false;

    record BaselineAtom(String name, int protocol, int atom, String type, Set<String> flags) {}

    @BeforeAll
    static void setUp() {
        registryAtoms = AtomRegistry.getAllAtoms();

        // Load baseline from resource
        try (InputStream is = AdaBinRegressionTest.class.getResourceAsStream("/ada-bin-baseline.json")) {
            if (is != null) {
                baselineAtoms = parseBaseline(is);
                baselineByName = baselineAtoms.stream()
                    .collect(Collectors.toMap(BaselineAtom::name, a -> a));
                baselineAvailable = true;
            }
        } catch (Exception e) {
            System.err.println("Failed to load baseline: " + e.getMessage());
        }
    }

    private static List<BaselineAtom> parseBaseline(InputStream is) throws Exception {
        List<BaselineAtom> atoms = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        boolean inAtoms = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("\"atoms\":")) {
                inAtoms = true;
                continue;
            }
            if (inAtoms && line.startsWith("{")) {
                // Parse: {"name": "...", "protocol": X, "atom": Y, "type": "...", "flags": [...]}
                String name = extractString(line, "name");
                int protocol = extractInt(line, "protocol");
                int atom = extractInt(line, "atom");
                String type = extractString(line, "type");
                Set<String> flags = extractFlags(line);

                if (name != null) {
                    atoms.add(new BaselineAtom(name, protocol, atom, type, flags));
                }
            }
        }
        return atoms;
    }

    private static String extractString(String line, String key) {
        String pattern = "\"" + key + "\": \"";
        int start = line.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        int end = line.indexOf("\"", start);
        return end > start ? line.substring(start, end) : null;
    }

    private static int extractInt(String line, String key) {
        String pattern = "\"" + key + "\": ";
        int start = line.indexOf(pattern);
        if (start < 0) return -1;
        start += pattern.length();
        int end = start;
        while (end < line.length() && Character.isDigit(line.charAt(end))) end++;
        return Integer.parseInt(line.substring(start, end));
    }

    private static Set<String> extractFlags(String line) {
        Set<String> flags = new HashSet<>();
        int start = line.indexOf("\"flags\": [");
        if (start < 0) return flags;
        int end = line.indexOf("]", start);
        if (end < 0) return flags;

        String flagsStr = line.substring(start + 10, end);
        for (String part : flagsStr.split(",")) {
            part = part.trim().replace("\"", "");
            if (!part.isEmpty()) {
                flags.add(part);
            }
        }
        return flags;
    }

    @Test
    void baselineShouldBeAvailable() {
        assertThat(baselineAvailable)
            .as("Baseline file ada-bin-baseline.json should be available")
            .isTrue();
    }

    @Test
    void shouldNotRemoveExistingAtoms() {
        if (!baselineAvailable) return;

        List<String> removed = new ArrayList<>();
        Map<String, AtomDefinition> registryByName = registryAtoms.stream()
            .collect(Collectors.toMap(AtomDefinition::name, a -> a));

        for (BaselineAtom baseline : baselineAtoms) {
            if (!registryByName.containsKey(baseline.name())) {
                removed.add(baseline.name());
            }
        }

        assertThat(removed)
            .as("No atoms from baseline should be removed - this would break existing code")
            .isEmpty();
    }

    @Test
    void shouldNotChangeAtomNumbers() {
        if (!baselineAvailable) return;

        List<String> changed = new ArrayList<>();
        Map<String, AtomDefinition> registryByName = registryAtoms.stream()
            .collect(Collectors.toMap(AtomDefinition::name, a -> a));

        for (BaselineAtom baseline : baselineAtoms) {
            AtomDefinition current = registryByName.get(baseline.name());
            if (current != null && current.atomNumber() != baseline.atom()) {
                changed.add(String.format("%s: baseline atom#=%d, current atom#=%d",
                    baseline.name(), baseline.atom(), current.atomNumber()));
            }
        }

        assertThat(changed)
            .as("Atom numbers must not change - this would break binary compatibility")
            .isEmpty();
    }

    @Test
    void shouldNotChangeProtocolNumbers() {
        if (!baselineAvailable) return;

        List<String> changed = new ArrayList<>();
        Map<String, AtomDefinition> registryByName = registryAtoms.stream()
            .collect(Collectors.toMap(AtomDefinition::name, a -> a));

        for (BaselineAtom baseline : baselineAtoms) {
            AtomDefinition current = registryByName.get(baseline.name());
            if (current != null && current.protocol() != baseline.protocol()) {
                changed.add(String.format("%s: baseline proto=%d, current proto=%d",
                    baseline.name(), baseline.protocol(), current.protocol()));
            }
        }

        assertThat(changed)
            .as("Protocol numbers must not change - this would break wire protocol")
            .isEmpty();
    }

    @Test
    void shouldReportNewAtoms() {
        if (!baselineAvailable) return;

        List<String> newAtoms = new ArrayList<>();

        for (AtomDefinition current : registryAtoms) {
            if (!baselineByName.containsKey(current.name())) {
                newAtoms.add(String.format("%s (proto=%d, atom=%d, type=%s)",
                    current.name(), current.protocol(), current.atomNumber(), current.type()));
            }
        }

        if (!newAtoms.isEmpty()) {
            System.out.println("INFO: " + newAtoms.size() + " new atoms added since baseline:");
            newAtoms.forEach(a -> System.out.println("  + " + a));
            System.out.println("Consider regenerating baseline if these are intentional additions.");
        }

        // Note: We don't fail on new atoms - additions are generally safe
        // Just report them for awareness
    }

    @Test
    void shouldReportTypeChanges() {
        if (!baselineAvailable) return;

        List<String> changed = new ArrayList<>();
        Map<String, AtomDefinition> registryByName = registryAtoms.stream()
            .collect(Collectors.toMap(AtomDefinition::name, a -> a));

        for (BaselineAtom baseline : baselineAtoms) {
            AtomDefinition current = registryByName.get(baseline.name());
            if (current != null && !current.type().name().equals(baseline.type())) {
                changed.add(String.format("%s: baseline type=%s, current type=%s",
                    baseline.name(), baseline.type(), current.type()));
            }
        }

        if (!changed.isEmpty()) {
            System.out.println("WARNING: " + changed.size() + " atoms changed type:");
            changed.forEach(c -> System.out.println("  ! " + c));
            System.out.println("Type changes may affect encoding/decoding - verify carefully.");
        }

        // Report but don't fail - type changes might be intentional fixes
        // But we want visibility into what changed
    }

    @Test
    void shouldMatchAtomCountWithinReason() {
        if (!baselineAvailable) return;

        int baselineCount = baselineAtoms.size();
        int currentCount = registryAtoms.size();

        // Allow up to 10% growth (new versions might add atoms)
        // But not shrinkage (that would indicate removals)
        assertThat(currentCount)
            .as("Atom count should not decrease from baseline")
            .isGreaterThanOrEqualTo(baselineCount);

        int maxGrowth = (int) (baselineCount * 1.10);
        if (currentCount > maxGrowth) {
            System.out.println("WARNING: Atom count increased significantly: " +
                baselineCount + " -> " + currentCount);
        }

        System.out.println("Atom count: baseline=" + baselineCount + ", current=" + currentCount);
    }

    @Test
    void criticalAtomsShouldExist() {
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

        Map<String, AtomDefinition> registryByName = registryAtoms.stream()
            .collect(Collectors.toMap(AtomDefinition::name, a -> a));

        for (String name : criticalAtoms) {
            assertThat(registryByName.containsKey(name))
                .as("Critical atom '%s' must exist in registry", name)
                .isTrue();
        }

        System.out.println("All " + criticalAtoms.length + " critical atoms verified present");
    }

    @Test
    void printRegressionSummary() {
        System.out.println("\n=== ADA.BIN Regression Test Summary ===");
        System.out.println("Baseline atoms: " + (baselineAvailable ? baselineAtoms.size() : "N/A"));
        System.out.println("Registry atoms: " + registryAtoms.size());

        if (baselineAvailable) {
            Set<String> baselineNames = baselineByName.keySet();
            Set<String> registryNames = registryAtoms.stream()
                .map(AtomDefinition::name)
                .collect(Collectors.toSet());

            int added = 0, removed = 0;
            for (String name : registryNames) {
                if (!baselineNames.contains(name)) added++;
            }
            for (String name : baselineNames) {
                if (!registryNames.contains(name)) removed++;
            }

            System.out.println("Added: " + added);
            System.out.println("Removed: " + removed);
            System.out.println("Status: " + (removed == 0 ? "OK" : "REGRESSION DETECTED"));
        }
    }
}
