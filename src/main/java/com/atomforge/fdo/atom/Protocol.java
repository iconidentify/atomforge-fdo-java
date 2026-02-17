package com.atomforge.fdo.atom;

/**
 * Protocol definitions.
 * Protocols 0-31 are sent over the wire, 32-127 are client-local only.
 */
public final class Protocol {
    private Protocol() {}

    // Core protocols
    public static final int UNI = 0;        // Universal stream control
    public static final int MAN = 1;        // Manager/display commands
    public static final int ACT = 2;        // Action handling
    public static final int DE = 3;         // Data extraction
    public static final int BUF = 4;        // Buffer operations
    public static final int IDB = 5;        // Internal database
    public static final int XFER = 7;       // File transfer
    public static final int FM = 8;         // File manager
    public static final int LM = 9;         // List manager
    public static final int CM = 10;        // Component manager
    public static final int CHAT = 11;      // Chat protocol
    public static final int VAR = 12;       // Variable operations
    public static final int ASYNC = 13;     // Async operations
    public static final int SM = 14;        // Shorthand manager
    public static final int IF = 15;        // Conditional atoms
    public static final int MAT = 16;       // Material attributes
    public static final int MIP = 17;       // Mail/IM protocol
    public static final int REG = 18;       // Registration
    public static final int FONT = 19;      // Font handling
    public static final int MMI = 20;       // Multimedia
    public static final int IMGXFER = 21;   // Image transfer
    public static final int IMAGE = 22;     // Image operations
    public static final int CHART = 23;     // Chart operations
    public static final int MORG = 24;      // Morgue/organizer
    public static final int RICH = 25;      // Rich text
    public static final int EXAPI = 26;     // External API
    public static final int DOD = 27;       // Data on demand
    public static final int RADIO = 28;     // Radio
    public static final int PICTALK = 29;   // Picture talk
    public static final int IRC = 30;       // IRC
    public static final int DOC = 31;       // Document
    public static final int VIDEO = 32;     // Video
    public static final int SND = 33;       // Sound
    public static final int CCL = 34;       // CCL
    public static final int P3 = 35;        // P3
    public static final int STATS = 36;     // Statistics
    public static final int PT = 37;        // Point-to-point
    public static final int PAKMAN = 38;    // Package manager
    public static final int AD = 39;        // Address book
    public static final int APP = 40;       // Application
    public static final int CONTEXT = 41;   // Context management
    public static final int MT = 42;        // Master tool
    public static final int DBRES = 43;     // Database resources
    public static final int MODEM = 45;     // Modem control
    public static final int TCP = 46;       // TCP/IP stack
    public static final int VRM = 47;       // VRM
    public static final int WWW = 48;       // Web
    public static final int AOLSOCK = 49;   // Socket layer
    public static final int PPP = 50;       // PPP protocol
    public static final int HFS = 51;       // HFS
    public static final int BLANK = 52;     // Blank
    public static final int VID = 53;       // Video (alternate)
    public static final int STARTUP = 54;   // Startup
    public static final int FAX = 55;       // Fax

    // Protocol ranges
    public static final int MIN_WIRE_PROTOCOL = 0;
    public static final int MAX_WIRE_PROTOCOL = 31;
    public static final int MIN_EXTENDED_PROTOCOL = 32;
    public static final int MAX_EXTENDED_PROTOCOL = 127;

    /**
     * Check if protocol is sent over the wire (0-31)
     */
    public static boolean isWireProtocol(int protocol) {
        return protocol >= MIN_WIRE_PROTOCOL && protocol <= MAX_WIRE_PROTOCOL;
    }

    /**
     * Check if protocol is extended/client-local (32-127)
     */
    public static boolean isExtendedProtocol(int protocol) {
        return protocol >= MIN_EXTENDED_PROTOCOL && protocol <= MAX_EXTENDED_PROTOCOL;
    }

    /**
     * Get protocol prefix for atom names
     */
    public static String getPrefix(int protocol) {
        return switch (protocol) {
            case UNI -> "uni";
            case MAN -> "man";
            case ACT -> "act";
            case DE -> "de";
            case BUF -> "buf";
            case IDB -> "idb";
            case XFER -> "xfer";
            case FM -> "fm";
            case LM -> "lm";
            case CM -> "cm";
            case CHAT -> "chat";
            case VAR -> "var";
            case ASYNC -> "async";
            case SM -> "sm";
            case IF -> "if";
            case MAT -> "mat";
            case MIP -> "mip";
            case REG -> "reg";
            case FONT -> "font";
            case MMI -> "mmi";
            case IMGXFER -> "imgxfer";
            case IMAGE -> "image";
            case CHART -> "chart";
            case MORG -> "morg";
            case RICH -> "rich";
            case EXAPI -> "exapi";
            case DOD -> "dod";
            case RADIO -> "radio";
            case PICTALK -> "pictalk";
            case IRC -> "irc";
            case DOC -> "doc";
            case VIDEO -> "vid";
            case SND -> "snd";
            case CCL -> "ccl";
            case P3 -> "p3";
            case STATS -> "stats";
            case PT -> "pt";
            case PAKMAN -> "pakman";
            case AD -> "ad";
            case APP -> "app";
            case CONTEXT -> "context";
            case MT -> "mt";
            case DBRES -> "dbres";
            case MODEM -> "modem";
            case TCP -> "tcp";
            case VRM -> "vrm";
            case WWW -> "www";
            case AOLSOCK -> "aolsock";
            case PPP -> "ppp";
            case HFS -> "hfs";
            case BLANK -> "blank";
            case VID -> "vid";
            case STARTUP -> "startup";
            case FAX -> "fax";
            default -> "proto" + protocol;
        };
    }
}
