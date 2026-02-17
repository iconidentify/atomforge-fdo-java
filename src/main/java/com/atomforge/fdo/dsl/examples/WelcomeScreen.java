package com.atomforge.fdo.dsl.examples;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.FdoScript;
import com.atomforge.fdo.dsl.StreamBuilder;
import com.atomforge.fdo.model.FdoGid;

/**
 * Example: Welcome Screen with dynamic headlines and action buttons.
 *
 * This demonstrates a complex FDO script with:
 * - Context manipulation (set_context_relative, set_context_globalid)
 * - Dynamic content replacement (replace_data with HTML)
 * - Action button configuration (replace_select_action with nested streams)
 * - Art/image assignments
 * - State machine token commands
 *
 * This replicates the classic welcome screen pattern with news headlines
 * that users can click to navigate to different sections.
 */
public final class WelcomeScreen {

    // Form GID that this welcome screen targets
    private static final FdoGid WELCOME_FORM_GID = FdoGid.of(32, 5447);
    private static final FdoGid FOCUS_GID = FdoGid.of(32, 30);

    // Art IDs for buttons
    private static final FdoGid ART_TECH = FdoGid.of(1, 0, 21029);
    private static final FdoGid ART_NEWS = FdoGid.of(1, 0, 21030);
    private static final FdoGid ART_FINANCIAL = FdoGid.of(1, 0, 21016);
    private static final FdoGid ART_SPORTS = FdoGid.of(1, 0, 21031);
    private static final FdoGid ART_ENTERTAINMENT = FdoGid.of(1, 0, 21025);

    // Context offsets for different sections
    private static final int CTX_TECH_HEADLINE = 3;
    private static final int CTX_TECH_BUTTON = 21;
    private static final int CTX_NEWS_HEADLINE = 10;
    private static final int CTX_FINANCIAL_HEADLINE = 12;
    private static final int CTX_SPORTS_HEADLINE = 14;
    private static final int CTX_ENTERTAINMENT_HEADLINE = 16;
    private static final int CTX_NEWS_BUTTON = 17;
    private static final int CTX_FINANCIAL_BUTTON = 18;
    private static final int CTX_SPORTS_BUTTON = 19;
    private static final int CTX_ENTERTAINMENT_BUTTON = 20;

    // Headlines and user data
    private String screenName = "User";
    private String techHeadline = "Latest tech news...";
    private String newsHeadline = "Breaking news...";
    private String financialHeadline = "Market updates...";
    private String sportsHeadline = "Sports scores...";
    private String entertainmentHeadline = "Entertainment news...";

    /**
     * Create a new WelcomeScreen builder.
     */
    public WelcomeScreen() {
    }

    /**
     * Set the user's screen name.
     */
    public WelcomeScreen screenName(String name) {
        this.screenName = name;
        return this;
    }

    /**
     * Set the tech section headline.
     */
    public WelcomeScreen techHeadline(String headline) {
        this.techHeadline = headline;
        return this;
    }

    /**
     * Set the news section headline.
     */
    public WelcomeScreen newsHeadline(String headline) {
        this.newsHeadline = headline;
        return this;
    }

    /**
     * Set the financial section headline.
     */
    public WelcomeScreen financialHeadline(String headline) {
        this.financialHeadline = headline;
        return this;
    }

    /**
     * Set the sports section headline.
     */
    public WelcomeScreen sportsHeadline(String headline) {
        this.sportsHeadline = headline;
        return this;
    }

    /**
     * Set the entertainment section headline.
     */
    public WelcomeScreen entertainmentHeadline(String headline) {
        this.entertainmentHeadline = headline;
        return this;
    }

    /**
     * Format a headline in HTML.
     */
    private String formatHeadline(String category, String headline) {
        return String.format(
            "<HTML><P ALIGN=LEFT><FONT SIZE=2><b>%s</b> %s</FONT></P><br></HTML>",
            category, headline
        );
    }

    /**
     * Add a button action that sends a token when clicked.
     */
    private void addButtonAction(StreamBuilder stream, int contextOffset, FdoGid artId, String token) throws FdoException {
        // Set art for the button
        stream.withContextRelative(contextOffset, ctx -> ctx.artId(artId));

        // Set the click action
        stream.setContextRelative(contextOffset);
        stream.replaceSelectAction(action ->
            action
                .startStreamWaitOn()
                .sendTokenArg(token)
                .endStream()
        );
        stream.endContext();
    }

    /**
     * Compile this WelcomeScreen to FDO binary format.
     *
     * @return The compiled binary FDO stream
     * @throws FdoException if compilation fails
     */
    public byte[] compile() throws FdoException {
        StreamBuilder stream = FdoScript.stream();

        // Start stream and initialize
        stream.startStream()
              .waitOn()
              .invokeLocal(WELCOME_FORM_GID);

        // Set window title
        stream.title("Welcome, " + screenName + "!");

        // Focus on the welcome form
        stream.setContextGlobalId(FOCUS_GID)
              .makeFocus();

        // Tech headline
        stream.withContextRelative(CTX_TECH_HEADLINE, ctx ->
            ctx.replaceData(formatHeadline("TECH", techHeadline))
               .endData()
        );

        // Tech button with art and action
        addButtonAction(stream, CTX_TECH_BUTTON, ART_TECH, "NX5");

        // News headline
        stream.withContextRelative(CTX_NEWS_HEADLINE, ctx ->
            ctx.replaceData(formatHeadline("NEWS", newsHeadline))
        );

        // Financial headline
        stream.withContextRelative(CTX_FINANCIAL_HEADLINE, ctx ->
            ctx.replaceData(formatHeadline("FINANCIAL", financialHeadline))
        );

        // Sports headline
        stream.withContextRelative(CTX_SPORTS_HEADLINE, ctx ->
            ctx.replaceData(formatHeadline("SPORTS", sportsHeadline))
        );

        // Entertainment headline
        stream.withContextRelative(CTX_ENTERTAINMENT_HEADLINE, ctx ->
            ctx.replaceData(formatHeadline("CULTURE", entertainmentHeadline))
        );

        // News button
        addButtonAction(stream, CTX_NEWS_BUTTON, ART_NEWS, "NX1");

        // Financial button
        addButtonAction(stream, CTX_FINANCIAL_BUTTON, ART_FINANCIAL, "NX3");

        // Sports button
        addButtonAction(stream, CTX_SPORTS_BUTTON, ART_SPORTS, "NX2");

        // Entertainment button
        addButtonAction(stream, CTX_ENTERTAINMENT_BUTTON, ART_ENTERTAINMENT, "NX4");

        // Force update and refresh display
        stream.forceUpdate()
              .updateDisplay();

        // Send tracking token
        stream.startStream()
              .sendTokenRaw("TO")
              .endStream();

        // End with wait off
        stream.waitOffEndStream();

        return stream.compile();
    }

    /**
     * Decompile this WelcomeScreen back to FDO text format.
     */
    public String decompile() throws FdoException {
        return FdoScript.decompile(compile());
    }

    // ========== Static factory methods ==========

    /**
     * Create a WelcomeScreen for the given user.
     */
    public static WelcomeScreen forUser(String screenName) {
        return new WelcomeScreen().screenName(screenName);
    }

    /**
     * Create a fully populated WelcomeScreen.
     */
    public static WelcomeScreen create(
            String screenName,
            String techHeadline,
            String newsHeadline,
            String financialHeadline,
            String sportsHeadline,
            String entertainmentHeadline
    ) {
        return new WelcomeScreen()
            .screenName(screenName)
            .techHeadline(techHeadline)
            .newsHeadline(newsHeadline)
            .financialHeadline(financialHeadline)
            .sportsHeadline(sportsHeadline)
            .entertainmentHeadline(entertainmentHeadline);
    }

    // ========== Example main for testing ==========

    public static void main(String[] args) throws FdoException {
        WelcomeScreen welcome = WelcomeScreen.create(
            "JohnDoe123",
            "AI Revolution: New breakthroughs in machine learning",
            "Global Summit: World leaders meet to discuss climate",
            "Markets Rally: Dow Jones hits new record high",
            "Championship Finals: Home team advances to playoffs",
            "Award Season: Nominations announced for top honors"
        );

        // Compile to binary
        byte[] binary = welcome.compile();
        System.out.println("Compiled to " + binary.length + " bytes");

        // Decompile back to text for verification
        String text = welcome.decompile();
        System.out.println("\nDecompiled FDO:\n" + text);
    }
}
