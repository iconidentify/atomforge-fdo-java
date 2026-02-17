package com.atomforge.fdo.dsl.examples;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.FdoScript;
import com.atomforge.fdo.dsl.values.*;
import com.atomforge.fdo.model.FdoGid;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Example: NewsStory window built using the FDO DSL.
 *
 * This is a Java equivalent of news_story.fdo.txt, demonstrating
 * how to use the type-safe DSL instead of template strings.
 *
 * The original FDO text used Mustache-style templates like {{WINDOW_TITLE}}.
 * In Java, we use proper parameters and method arguments instead.
 */
public final class NewsStory {

    // Window dimensions
    private static final int WINDOW_WIDTH = 580;
    private static final int WINDOW_HEIGHT = 360;

    // Header ornament positioning
    private static final int HEADER_X = 20;
    private static final int HEADER_Y = 12;
    private static final int HEADER_WIDTH = 540;
    private static final int HEADER_HEIGHT = 20;

    // Content view positioning
    private static final int CONTENT_X = 20;
    private static final int CONTENT_Y = 40;
    private static final int CONTENT_WIDTH = 540;
    private static final int CONTENT_HEIGHT = 300;
    private static final int CONTENT_CAPACITY = 8192;

    // Default background art
    private static final FdoGid DEFAULT_BACKGROUND_ART = FdoGid.of(1, 69, 27256);

    private String windowTitle = "News";
    private String headerText;
    private String contentData = "";
    private FdoGid backgroundArt = DEFAULT_BACKGROUND_ART;
    private LocalDate date = LocalDate.now();

    /**
     * Create a new NewsStory builder.
     */
    public NewsStory() {
    }

    /**
     * Set the window title.
     */
    public NewsStory windowTitle(String title) {
        this.windowTitle = title;
        return this;
    }

    /**
     * Set the date to display in the header.
     */
    public NewsStory date(LocalDate date) {
        this.date = date;
        return this;
    }

    /**
     * Set custom header text. If not set, uses "title - date" format.
     */
    public NewsStory headerText(String text) {
        this.headerText = text;
        return this;
    }

    /**
     * Set the main content/body text.
     */
    public NewsStory content(String content) {
        this.contentData = content;
        return this;
    }

    /**
     * Set a custom background art GID.
     */
    public NewsStory backgroundArt(FdoGid art) {
        this.backgroundArt = art;
        return this;
    }

    /**
     * Set background art from components.
     */
    public NewsStory backgroundArt(int type, int subtype, int id) {
        this.backgroundArt = FdoGid.of(type, subtype, id);
        return this;
    }

    /**
     * Build the effective header text.
     */
    private String buildHeaderText() {
        if (headerText != null) {
            return headerText;
        }
        String formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        return windowTitle + " - " + formattedDate;
    }

    /**
     * Compile this NewsStory to FDO binary format.
     *
     * @return The compiled binary FDO stream
     * @throws FdoException if compilation fails
     */
    public byte[] compile() throws FdoException {
        return FdoScript.stream()
            // Main window container
            .startObject(ObjectType.IND_GROUP, "News")
                .orientation(Orientation.VCF)
                .position(Position.CENTER_CENTER)
                .precise()
                .preciseWidth(WINDOW_WIDTH)
                .preciseHeight(WINDOW_HEIGHT)
                .backgroundTile()
                .artId(backgroundArt)
                .backgroundPic()
                .title(windowTitle)

                // Header ornament with title and date
                .startObject(ObjectType.ORNAMENT, "")
                    .preciseBounds(HEADER_X, HEADER_Y, HEADER_WIDTH, HEADER_HEIGHT)
                    .fontId(FontId.ARIAL)
                    .fontSize(14)
                    .fontSis(FontId.ARIAL, 16, FontStyle.BOLD)
                    .titlePos(TitlePosition.ABOVE_CENTER)
                    .appendData(buildHeaderText())
                    .endData()
                .endObject()

                // Main content view - scrollable, read-only
                .startObject(ObjectType.VIEW, "")
                    .preciseBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT)
                    .capacity(CONTENT_CAPACITY)
                    .writeable(false)
                    .verticalScroll()
                    .fontId(FontId.ARIAL)
                    .fontSize(10)
                    .appendData(contentData)
                    .endData()
                .endObject()

                .updateDisplay()
            .endObject()
            .waitOff()
            .compile();
    }

    /**
     * Decompile this NewsStory back to FDO text format.
     * Useful for debugging and verification.
     */
    public String decompile() throws FdoException {
        return FdoScript.decompile(compile());
    }

    // ========== Static factory methods ==========

    /**
     * Create a simple news story with title and content.
     */
    public static NewsStory create(String title, String content) {
        return new NewsStory()
            .windowTitle(title)
            .content(content);
    }

    /**
     * Create a news story with all parameters.
     */
    public static NewsStory create(String title, LocalDate date, String content) {
        return new NewsStory()
            .windowTitle(title)
            .date(date)
            .content(content);
    }

    // ========== Example main for testing ==========

    public static void main(String[] args) throws FdoException {
        // Example usage
        NewsStory story = NewsStory.create(
            "Breaking News",
            "This is an example news story created using the FDO DSL.\n\n" +
            "The DSL provides type-safe, IDE-friendly construction of FDO streams.\n\n" +
            "Features:\n" +
            "- Compile-time type checking\n" +
            "- IDE autocomplete\n" +
            "- Human-readable enums\n" +
            "- Fluent builder pattern"
        );

        // Compile to binary
        byte[] binary = story.compile();
        System.out.println("Compiled to " + binary.length + " bytes");

        // Decompile back to text for verification
        String text = story.decompile();
        System.out.println("\nDecompiled FDO:\n" + text);
    }
}
