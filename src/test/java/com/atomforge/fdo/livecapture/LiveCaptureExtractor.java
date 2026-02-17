package com.atomforge.fdo.livecapture;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Extracts FDO scripts from live capture log files.
 *
 * The log format contains FDO scripts that appear after lines matching:
 *   [timestamp][INFO] processed fdo: uni_start_stream
 *
 * And end with various terminators like:
 *   - uni_end_stream (standalone)
 *   - man_update_woff_end_stream
 *   - uni_wait_off_end_stream
 *   - Followed by [timestamp] on the next line
 */
public class LiveCaptureExtractor {

    private static final Pattern PROCESSED_FDO_PATTERN = Pattern.compile(
        "^\\[.*?\\]\\[INFO\\] processed fdo: (.*)$"
    );

    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
        "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+\\]\\[.*?\\].*$"
    );

    public static void main(String[] args) throws IOException {
        Path logFile = Path.of("livecapture_reearch/rawlog.txt");
        Path outputDir = Path.of("livecapture_reearch/extracted");

        if (!Files.exists(logFile)) {
            System.err.println("Log file not found: " + logFile);
            System.exit(1);
        }

        Files.createDirectories(outputDir);

        List<String> lines = Files.readAllLines(logFile);
        List<ExtractedScript> scripts = extractScripts(lines);

        System.out.println("Extracted " + scripts.size() + " FDO scripts");

        for (int i = 0; i < scripts.size(); i++) {
            ExtractedScript script = scripts.get(i);
            String filename = String.format("%03d-%s.fdo.txt", i + 1, sanitizeFilename(script.description()));
            Path outputPath = outputDir.resolve(filename);
            Files.writeString(outputPath, script.content());
            System.out.printf("  %s (%d lines)%n", filename, script.lineCount());
        }

        System.out.println("\nScripts written to: " + outputDir);
    }

    private static List<ExtractedScript> extractScripts(List<String> lines) {
        List<ExtractedScript> scripts = new ArrayList<>();

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            Matcher matcher = PROCESSED_FDO_PATTERN.matcher(line);

            if (matcher.matches()) {
                String startAtom = matcher.group(1).trim();
                StringBuilder content = new StringBuilder();
                content.append(startAtom).append("\n");

                i++;
                int lineCount = 1;

                // Collect lines until we hit a terminator or new log entry
                while (i < lines.size()) {
                    String currentLine = lines.get(i);

                    // Check if this is a new log line (marks end of FDO)
                    if (LOG_LINE_PATTERN.matcher(currentLine).matches()) {
                        break;
                    }

                    content.append(currentLine).append("\n");
                    lineCount++;
                    i++;
                }

                // Clean up the content
                String cleanedContent = cleanContent(content.toString());
                String description = extractDescription(startAtom, cleanedContent);

                scripts.add(new ExtractedScript(description, cleanedContent, lineCount));
            } else {
                i++;
            }
        }

        return scripts;
    }

    private static String cleanContent(String content) {
        // Remove trailing whitespace from each line but preserve structure
        String[] lines = content.split("\n", -1);
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            cleaned.append(line.stripTrailing()).append("\n");
        }

        // Remove trailing newlines
        String result = cleaned.toString();
        while (result.endsWith("\n\n")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private static String extractDescription(String startAtom, String content) {
        // Try to extract a meaningful description from the content

        // Look for template hints in common patterns
        if (content.contains("idb_set_context")) return "idb_context";
        if (content.contains("async_set_screen_name")) return "async_screen_name";
        if (content.contains("idb_delete_obj")) return "idb_delete";
        if (content.contains("mat_title")) {
            // Extract title if possible
            Pattern titlePattern = Pattern.compile("mat_title <\"([^\"]+)\">");
            Matcher m = titlePattern.matcher(content);
            if (m.find()) {
                String title = m.group(1);
                if (title.length() > 30) title = title.substring(0, 30);
                return "title_" + sanitizeFilename(title);
            }
            return "mat_title";
        }
        if (content.contains("idb_start_obj")) return "idb_start_obj";
        if (content.contains("dod_start")) return "dod_response";
        if (content.contains("sm_popup_im")) return "popup_im";
        if (content.contains("mat_art_id")) return "art_id";
        if (content.contains("man_replace_data")) return "replace_data";
        if (content.contains("buf_start_buffer")) return "buffer";

        // Default to the start atom name
        return startAtom.replace("uni_", "").replace("_", "");
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "")
                   .toLowerCase();
    }

    record ExtractedScript(String description, String content, int lineCount) {}
}
