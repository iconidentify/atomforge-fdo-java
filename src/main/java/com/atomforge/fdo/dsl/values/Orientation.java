package com.atomforge.fdo.dsl.values;

/**
 * FDO orientation codes for mat_orientation atoms.
 *
 * Orientation is encoded as a single byte with bit-packed fields:
 * - Bit 6: vertical (1) or horizontal (0)
 * - Bits 5-3: horizontal justify (center=0, left=1, right=2, full=3, even=4)
 * - Bits 2-0: vertical justify (center=0, top=1, bottom=2, full=3, even=4)
 *
 * Common orientations have 3-letter codes like "vcf" (vertical, center, full).
 */
public enum Orientation {
    // Horizontal orientations
    HCC(0x00, "hcc"),  // horizontal, center, center
    HCT(0x01, "hct"),  // horizontal, center, top
    HCB(0x02, "hcb"),  // horizontal, center, bottom
    HCF(0x03, "hcf"),  // horizontal, center, full
    HCE(0x04, "hce"),  // horizontal, center, even
    HLC(0x08, "hlc"),  // horizontal, left, center
    HLT(0x09, "hlt"),  // horizontal, left, top
    HLB(0x0A, "hlb"),  // horizontal, left, bottom
    HLF(0x0B, "hlf"),  // horizontal, left, full
    HLE(0x0C, "hle"),  // horizontal, left, even
    HRC(0x10, "hrc"),  // horizontal, right, center
    HRT(0x11, "hrt"),  // horizontal, right, top
    HRB(0x12, "hrb"),  // horizontal, right, bottom
    HRF(0x13, "hrf"),  // horizontal, right, full
    HRE(0x14, "hre"),  // horizontal, right, even
    HFC(0x18, "hfc"),  // horizontal, full, center
    HFT(0x19, "hft"),  // horizontal, full, top
    HFB(0x1A, "hfb"),  // horizontal, full, bottom
    HFF(0x1B, "hff"),  // horizontal, full, full
    HFE(0x1C, "hfe"),  // horizontal, full, even
    HEC(0x20, "hec"),  // horizontal, even, center
    HET(0x21, "het"),  // horizontal, even, top
    HEB(0x22, "heb"),  // horizontal, even, bottom
    HEF(0x23, "hef"),  // horizontal, even, full
    HEE(0x24, "hee"),  // horizontal, even, even

    // Vertical orientations
    VCC(0x40, "vcc"),  // vertical, center, center
    VCT(0x41, "vct"),  // vertical, center, top
    VCB(0x42, "vcb"),  // vertical, center, bottom
    VCF(0x43, "vcf"),  // vertical, center, full
    VCE(0x44, "vce"),  // vertical, center, even
    VLC(0x48, "vlc"),  // vertical, left, center
    VLT(0x49, "vlt"),  // vertical, left, top
    VLB(0x4A, "vlb"),  // vertical, left, bottom
    VLF(0x4B, "vlf"),  // vertical, left, full
    VLE(0x4C, "vle"),  // vertical, left, even
    VRC(0x50, "vrc"),  // vertical, right, center
    VRT(0x51, "vrt"),  // vertical, right, top
    VRB(0x52, "vrb"),  // vertical, right, bottom
    VRF(0x53, "vrf"),  // vertical, right, full
    VRE(0x54, "vre"),  // vertical, right, even
    VFC(0x58, "vfc"),  // vertical, full, center
    VFT(0x59, "vft"),  // vertical, full, top
    VFB(0x5A, "vfb"),  // vertical, full, bottom
    VFF(0x5B, "vff"),  // vertical, full, full
    VFE(0x5C, "vfe"),  // vertical, full, even
    VEC(0x60, "vec"),  // vertical, even, center
    VET(0x61, "vet"),  // vertical, even, top
    VEB(0x62, "veb"),  // vertical, even, bottom
    VEF(0x63, "vef"),  // vertical, even, full
    VEE(0x64, "vee");  // vertical, even, even

    private final int code;
    private final String fdoName;

    Orientation(int code, String fdoName) {
        this.code = code;
        this.fdoName = fdoName;
    }

    /**
     * @return The binary code used in FDO format
     */
    public int code() {
        return code;
    }

    /**
     * @return The 3-letter name used in FDO text format
     */
    public String fdoName() {
        return fdoName;
    }

    /**
     * @return True if this is a vertical orientation
     */
    public boolean isVertical() {
        return (code & 0x40) != 0;
    }

    /**
     * @return The horizontal justify component
     */
    public HorizontalJustify horizontalJustify() {
        int hj = (code >> 3) & 0x07;
        return HorizontalJustify.values()[hj];
    }

    /**
     * @return The vertical justify component
     */
    public VerticalJustify verticalJustify() {
        int vj = code & 0x07;
        return VerticalJustify.values()[vj];
    }

    /**
     * Look up an Orientation by its binary code.
     * @param code The binary code
     * @return The matching Orientation, or null if not a standard orientation
     */
    public static Orientation fromCode(int code) {
        for (Orientation orient : values()) {
            if (orient.code == code) {
                return orient;
            }
        }
        return null;
    }

    /**
     * Look up an Orientation by its 3-letter FDO name.
     * @param name The FDO name (e.g., "vcf", "hlt")
     * @return The matching Orientation, or null if not found
     */
    public static Orientation fromName(String name) {
        if (name == null || name.length() != 3) return null;
        String lower = name.toLowerCase();
        for (Orientation orient : values()) {
            if (orient.fdoName.equals(lower)) {
                return orient;
            }
        }
        return null;
    }

    /**
     * Create an Orientation from its components.
     * @param vertical True for vertical, false for horizontal
     * @param horizontal The horizontal justify
     * @param vertical2 The vertical justify
     * @return The matching Orientation, or null if not a standard combination
     */
    public static Orientation of(boolean vertical, HorizontalJustify horizontal, VerticalJustify vertical2) {
        int code = (vertical ? 0x40 : 0) | (horizontal.ordinal() << 3) | vertical2.ordinal();
        return fromCode(code);
    }

    /**
     * Horizontal justify component.
     */
    public enum HorizontalJustify {
        CENTER, LEFT, RIGHT, FULL, EVEN
    }

    /**
     * Vertical justify component.
     */
    public enum VerticalJustify {
        CENTER, TOP, BOTTOM, FULL, EVEN
    }
}
