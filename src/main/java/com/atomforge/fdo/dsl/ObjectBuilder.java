package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.atoms.*;
import com.atomforge.fdo.dsl.values.*;
import com.atomforge.fdo.model.FdoGid;

import java.util.function.Consumer;

/**
 * Fluent builder for configuring FDO objects.
 *
 * This builder is returned from StreamBuilder.startObject() and provides
 * convenient methods for setting common object attributes.
 *
 * Example usage:
 * <pre>{@code
 * FdoScript.stream()
 *     .startObject(ObjectType.TRIGGER, "OK Button")
 *         .position(Position.BOTTOM_CENTER)
 *         .triggerStyle(TriggerStyle.FRAMED)
 *         .fontSis(FontId.ARIAL, 10, FontStyle.BOLD)
 *         .onSelect(action -> action.data("OK"))
 *     .endObject()
 *     .compile();
 * }</pre>
 */
public final class ObjectBuilder {

    private final StreamBuilder stream;

    /**
     * Create an ObjectBuilder wrapping the given stream.
     */
    ObjectBuilder(StreamBuilder stream) {
        this.stream = stream;
    }

    // ========== MAT Attribute Methods ==========

    /**
     * Set the orientation (mat_orientation).
     */
    public ObjectBuilder orientation(Orientation orient) {
        stream.orientation(orient);
        return this;
    }

    /**
     * Set the position (mat_position).
     */
    public ObjectBuilder position(Position pos) {
        stream.position(pos);
        return this;
    }

    /**
     * Set the title position (mat_title_pos).
     */
    public ObjectBuilder titlePos(TitlePosition pos) {
        stream.titlePos(pos);
        return this;
    }

    /**
     * Set the font ID (mat_font_id).
     */
    public ObjectBuilder fontId(FontId font) {
        stream.fontId(font);
        return this;
    }

    /**
     * Set the font size (mat_font_size).
     */
    public ObjectBuilder fontSize(int size) {
        stream.fontSize(size);
        return this;
    }

    /**
     * Set the font style (mat_font_style).
     */
    public ObjectBuilder fontStyle(FontStyle style) {
        stream.fontStyle(style);
        return this;
    }

    /**
     * Set font ID, size, and style together (mat_font_sis).
     */
    public ObjectBuilder fontSis(FontId font, int size, FontStyle style) {
        stream.fontSis(font, size, style);
        return this;
    }

    /**
     * Set the frame style (mat_frame_style).
     */
    public ObjectBuilder frameStyle(FrameStyle style) {
        stream.frameStyle(style);
        return this;
    }

    /**
     * Set the trigger style (mat_trigger_style).
     */
    public ObjectBuilder triggerStyle(TriggerStyle style) {
        stream.triggerStyle(style);
        return this;
    }

    /**
     * Set the size (mat_size) with rows and columns.
     */
    public ObjectBuilder size(int rows, int cols) {
        stream.size(rows, cols);
        return this;
    }

    /**
     * Set the size (mat_size) with rows, columns, and max characters.
     */
    public ObjectBuilder size(int rows, int cols, int maxChars) {
        stream.size(rows, cols, maxChars);
        return this;
    }

    /**
     * Set the log type (mat_log_object).
     */
    public ObjectBuilder logType(LogType type) {
        stream.logType(type);
        return this;
    }

    /**
     * Set the sort order (mat_sort_order).
     */
    public ObjectBuilder sortOrder(SortOrder order) {
        stream.sortOrder(order);
        return this;
    }

    /**
     * Mark as disabled (mat_bool_disabled).
     */
    public ObjectBuilder disabled() {
        stream.disabled();
        return this;
    }

    /**
     * Mark as invisible (mat_bool_invisible).
     */
    public ObjectBuilder invisible() {
        stream.invisible();
        return this;
    }

    /**
     * Mark as modal (mat_bool_modal).
     */
    public ObjectBuilder modal() {
        stream.modal();
        return this;
    }

    // ========== Precise positioning ==========

    /**
     * Enable precise positioning mode (mat_bool_precise).
     */
    public ObjectBuilder precise() {
        stream.precise();
        return this;
    }

    /**
     * Set precise width in pixels (mat_precise_width).
     */
    public ObjectBuilder preciseWidth(int width) {
        stream.preciseWidth(width);
        return this;
    }

    /**
     * Set precise height in pixels (mat_precise_height).
     */
    public ObjectBuilder preciseHeight(int height) {
        stream.preciseHeight(height);
        return this;
    }

    /**
     * Set precise X position in pixels (mat_precise_x).
     */
    public ObjectBuilder preciseX(int x) {
        stream.preciseX(x);
        return this;
    }

    /**
     * Set precise Y position in pixels (mat_precise_y).
     */
    public ObjectBuilder preciseY(int y) {
        stream.preciseY(y);
        return this;
    }

    /**
     * Set precise bounds (x, y, width, height) in one call.
     */
    public ObjectBuilder preciseBounds(int x, int y, int width, int height) {
        stream.preciseBounds(x, y, width, height);
        return this;
    }

    // ========== Background styling ==========

    /**
     * Enable background tiling (mat_bool_background_tile).
     */
    public ObjectBuilder backgroundTile() {
        stream.backgroundTile();
        return this;
    }

    /**
     * Enable background picture (mat_bool_background_pic).
     */
    public ObjectBuilder backgroundPic() {
        stream.backgroundPic();
        return this;
    }

    /**
     * Set the art/image ID (mat_art_id).
     */
    public ObjectBuilder artId(FdoGid gid) {
        stream.artId(gid);
        return this;
    }

    /**
     * Set the art/image ID from components (mat_art_id).
     */
    public ObjectBuilder artId(int type, int subtype, int id) {
        stream.artId(type, subtype, id);
        return this;
    }

    // ========== Title and capacity ==========

    /**
     * Set the window/object title (mat_title).
     */
    public ObjectBuilder title(String title) {
        stream.title(title);
        return this;
    }

    /**
     * Set the capacity/buffer size (mat_capacity).
     */
    public ObjectBuilder capacity(int capacity) {
        stream.capacity(capacity);
        return this;
    }

    // ========== Scrolling and edit behavior ==========

    /**
     * Enable vertical scrolling (mat_bool_vertical_scroll).
     */
    public ObjectBuilder verticalScroll() {
        stream.verticalScroll();
        return this;
    }

    /**
     * Enable horizontal scrolling (mat_bool_horizontal_scroll).
     */
    public ObjectBuilder horizontalScroll() {
        stream.horizontalScroll();
        return this;
    }

    /**
     * Set writeable flag (mat_bool_writeable).
     * @param writeable true to allow writing, false for read-only
     */
    public ObjectBuilder writeable(boolean writeable) {
        stream.writeable(writeable);
        return this;
    }

    // ========== Data operations ==========

    /**
     * Append data to this object (man_append_data) with string.
     */
    public ObjectBuilder appendData(String data) {
        stream.appendData(data);
        return this;
    }

    /**
     * Append data to this object (man_append_data) with raw bytes.
     */
    public ObjectBuilder appendData(byte[] data) {
        stream.appendData(data);
        return this;
    }

    /**
     * End data block (man_end_data).
     */
    public ObjectBuilder endData() {
        stream.endData();
        return this;
    }

    // ========== Data Methods ==========

    /**
     * Add data (de_data) with a string value.
     */
    public ObjectBuilder data(String value) {
        stream.data(value);
        return this;
    }

    /**
     * Add data (de_data) with a GID value.
     */
    public ObjectBuilder data(FdoGid gid) {
        stream.data(gid);
        return this;
    }

    /**
     * Add data (de_data) with a number value.
     */
    public ObjectBuilder data(long value) {
        stream.data(value);
        return this;
    }

    // ========== Raw MAT atom access ==========

    /**
     * Add any MAT protocol atom.
     */
    public ObjectBuilder mat(MatAtom atom, Object... args) {
        stream.mat(atom, args);
        return this;
    }

    /**
     * Add any DE protocol atom.
     */
    public ObjectBuilder de(DeAtom atom, Object... args) {
        stream.de(atom, args);
        return this;
    }

    /**
     * Add any atom.
     */
    public ObjectBuilder atom(DslAtom atom, Object... args) {
        stream.atom(atom, args);
        return this;
    }

    // ========== Typed MAT Methods (complete - mirrors StreamBuilder) ==========

    // --- Enum-typed attributes ---
    public ObjectBuilder matOrientation(Orientation orientation) { stream.matOrientation(orientation); return this; }
    public ObjectBuilder matPosition(Position position) { stream.matPosition(position); return this; }
    public ObjectBuilder matFontId(FontId fontId) { stream.matFontId(fontId); return this; }
    public ObjectBuilder matFontStyle(FontStyle fontStyle) { stream.matFontStyle(fontStyle); return this; }
    public ObjectBuilder matTitlePos(TitlePosition titlePos) { stream.matTitlePos(titlePos); return this; }
    public ObjectBuilder matLogObject(LogType logType) { stream.matLogObject(logType); return this; }
    public ObjectBuilder matSortOrder(SortOrder sortOrder) { stream.matSortOrder(sortOrder); return this; }
    public ObjectBuilder matFrameStyle(FrameStyle style) { stream.matFrameStyle(style); return this; }
    public ObjectBuilder matTriggerStyle(TriggerStyle style) { stream.matTriggerStyle(style); return this; }
    public ObjectBuilder matObjectType(ObjectType type) { stream.matObjectType(type); return this; }

    // --- Size and dimensions ---
    public ObjectBuilder matSize(int rows, int cols) { stream.matSize(rows, cols); return this; }
    public ObjectBuilder matSize(int rows, int cols, int maxChars) { stream.matSize(rows, cols, maxChars); return this; }
    public ObjectBuilder matWidth(int width) { stream.matWidth(width); return this; }
    public ObjectBuilder matHeight(int height) { stream.matHeight(height); return this; }
    public ObjectBuilder matCapacity(int capacity) { stream.matCapacity(capacity); return this; }
    public ObjectBuilder matPreciseX(int x) { stream.matPreciseX(x); return this; }
    public ObjectBuilder matPreciseY(int y) { stream.matPreciseY(y); return this; }
    public ObjectBuilder matPreciseWidth(int width) { stream.matPreciseWidth(width); return this; }
    public ObjectBuilder matPreciseHeight(int height) { stream.matPreciseHeight(height); return this; }

    // --- Title ---
    public ObjectBuilder matTitle(String title) { stream.matTitle(title); return this; }
    public ObjectBuilder matTitleWidth(int width) { stream.matTitleWidth(width); return this; }
    public ObjectBuilder matTitleAccess(int access) { stream.matTitleAccess(access); return this; }
    public ObjectBuilder matTitleAppendScreenName(String name) { stream.matTitleAppendScreenName(name); return this; }

    // --- Font ---
    public ObjectBuilder matFontSize(int size) { stream.matFontSize(size); return this; }
    public ObjectBuilder matFontSis(FontId font, int size, FontStyle style) { stream.matFontSis(font, size, style); return this; }
    public ObjectBuilder matIntlFontSis(int intl, FontId font, int size, FontStyle style) { stream.matIntlFontSis(intl, font, size, style); return this; }

    // --- Values and ranges ---
    public ObjectBuilder matValue(int value) { stream.matValue(value); return this; }
    public ObjectBuilder matMinimum(int min) { stream.matMinimum(min); return this; }
    public ObjectBuilder matMaximum(int max) { stream.matMaximum(max); return this; }
    public ObjectBuilder matIncrement(int increment) { stream.matIncrement(increment); return this; }

    // --- Scrolling ---
    public ObjectBuilder matHorzScrollValue(int value) { stream.matHorzScrollValue(value); return this; }
    public ObjectBuilder matVertScrollValue(int value) { stream.matVertScrollValue(value); return this; }
    public ObjectBuilder matHorzScrollCapacity(int capacity) { stream.matHorzScrollCapacity(capacity); return this; }
    public ObjectBuilder matVertScrollCapacity(int capacity) { stream.matVertScrollCapacity(capacity); return this; }
    public ObjectBuilder matScrollThreshold(int threshold) { stream.matScrollThreshold(threshold); return this; }

    // --- Colors ---
    public ObjectBuilder matColorFace(int color) { stream.matColorFace(color); return this; }
    public ObjectBuilder matColorText(int color) { stream.matColorText(color); return this; }
    public ObjectBuilder matColorTopEdge(int color) { stream.matColorTopEdge(color); return this; }
    public ObjectBuilder matColorBottomEdge(int color) { stream.matColorBottomEdge(color); return this; }
    public ObjectBuilder matColorSelected(int color) { stream.matColorSelected(color); return this; }
    public ObjectBuilder matColorTextShadow(int color) { stream.matColorTextShadow(color); return this; }
    public ObjectBuilder matColorFrameHilight(int color) { stream.matColorFrameHilight(color); return this; }
    public ObjectBuilder matColorFrameShadow(int color) { stream.matColorFrameShadow(color); return this; }
    public ObjectBuilder matColorThumb(int color) { stream.matColorThumb(color); return this; }
    public ObjectBuilder matColorChannel(int color) { stream.matColorChannel(color); return this; }

    // --- Art/images ---
    public ObjectBuilder matArtId(FdoGid gid) { stream.matArtId(gid); return this; }
    public ObjectBuilder matArtFrame(FdoGid gid) { stream.matArtFrame(gid); return this; }
    public ObjectBuilder matArtAnimationRate(int rate) { stream.matArtAnimationRate(rate); return this; }
    public ObjectBuilder matArtAnimationSeq(int seq) { stream.matArtAnimationSeq(seq); return this; }
    public ObjectBuilder matArtSeq(int seq) { stream.matArtSeq(seq); return this; }
    public ObjectBuilder matFormIcon(FdoGid gid) { stream.matFormIcon(gid); return this; }
    public ObjectBuilder matArtHintWidth(int width) { stream.matArtHintWidth(width); return this; }
    public ObjectBuilder matArtHintHeight(int height) { stream.matArtHintHeight(height); return this; }
    public ObjectBuilder matArtHintTitle(String title) { stream.matArtHintTitle(title); return this; }
    public ObjectBuilder matArtHintPlaceholderId(int id) { stream.matArtHintPlaceholderId(id); return this; }

    // --- GID references ---
    public ObjectBuilder matStyleId(FdoGid gid) { stream.matStyleId(gid); return this; }
    public ObjectBuilder matObjectId(FdoGid gid) { stream.matObjectId(gid); return this; }
    public ObjectBuilder matManagedBy(FdoGid gid) { stream.matManagedBy(gid); return this; }
    public ObjectBuilder matFactoryId(FdoGid gid) { stream.matFactoryId(gid); return this; }
    public ObjectBuilder matNavarrowArt(FdoGid gid) { stream.matNavarrowArt(gid); return this; }

    // --- URLs ---
    public ObjectBuilder matUrl(String url) { stream.matUrl(url); return this; }
    public ObjectBuilder matUrlNext(String url) { stream.matUrlNext(url); return this; }
    public ObjectBuilder matUrlPrev(String url) { stream.matUrlPrev(url); return this; }
    public ObjectBuilder matUrlParent(String url) { stream.matUrlParent(url); return this; }
    public ObjectBuilder matUrlList(String urls) { stream.matUrlList(urls); return this; }

    // --- Timers ---
    public ObjectBuilder matTimerEvent(int event) { stream.matTimerEvent(event); return this; }
    public ObjectBuilder matTimerDuration(int duration) { stream.matTimerDuration(duration); return this; }

    // --- Spacing ---
    public ObjectBuilder matVerticalSpacing(int spacing) { stream.matVerticalSpacing(spacing); return this; }
    public ObjectBuilder matHorizontalSpacing(int spacing) { stream.matHorizontalSpacing(spacing); return this; }
    public ObjectBuilder matSpacing(int spacing) { stream.matSpacing(spacing); return this; }
    public ObjectBuilder matLeftSpacing(int spacing) { stream.matLeftSpacing(spacing); return this; }
    public ObjectBuilder matTopSpacing(int spacing) { stream.matTopSpacing(spacing); return this; }
    public ObjectBuilder matRightSpacing(int spacing) { stream.matRightSpacing(spacing); return this; }
    public ObjectBuilder matBottomSpacing(int spacing) { stream.matBottomSpacing(spacing); return this; }

    // --- Object/tag identifiers ---
    public ObjectBuilder matRelativeTag(int tag) { stream.matRelativeTag(tag); return this; }
    public ObjectBuilder matObjectIndex(int index) { stream.matObjectIndex(index); return this; }

    // --- Layout/formatting ---
    public ObjectBuilder matRuler(int ruler) { stream.matRuler(ruler); return this; }
    public ObjectBuilder matBorder(int border) { stream.matBorder(border); return this; }
    public ObjectBuilder matParagraph(int para) { stream.matParagraph(para); return this; }

    // --- Keys/shortcuts ---
    public ObjectBuilder matCommandKey(int key) { stream.matCommandKey(key); return this; }
    public ObjectBuilder matShortcutKey(int key) { stream.matShortcutKey(key); return this; }

    // --- Validation/behavior ---
    public ObjectBuilder matValidation(int val) { stream.matValidation(val); return this; }
    public ObjectBuilder matIconifyAs(int iconify) { stream.matIconifyAs(iconify); return this; }
    public ObjectBuilder matPlusGroup(int group) { stream.matPlusGroup(group); return this; }
    public ObjectBuilder matUnfocused(int val) { stream.matUnfocused(val); return this; }
    public ObjectBuilder matDirtyQuery(int query) { stream.matDirtyQuery(query); return this; }
    public ObjectBuilder matLinkContentToRid(int rid) { stream.matLinkContentToRid(rid); return this; }

    // --- Help/context ---
    public ObjectBuilder matContextHelp(String help) { stream.matContextHelp(help); return this; }
    public ObjectBuilder matSageContextHelp(String help) { stream.matSageContextHelp(help); return this; }

    // --- Data/encoding ---
    public ObjectBuilder matDropDataType(int type) { stream.matDropDataType(type); return this; }
    public ObjectBuilder matFieldScript(String script) { stream.matFieldScript(script); return this; }
    public ObjectBuilder matEncodeType(int type) { stream.matEncodeType(type); return this; }
    public ObjectBuilder matSink(int sink) { stream.matSink(sink); return this; }
    public ObjectBuilder matContentTag(String tag) { stream.matContentTag(tag); return this; }
    public ObjectBuilder matLanguageSensitive(int lang) { stream.matLanguageSensitive(lang); return this; }
    public ObjectBuilder matSetDfltExtractType(int type) { stream.matSetDfltExtractType(type); return this; }

    // --- Security ---
    public ObjectBuilder matSecureForm(int secure) { stream.matSecureForm(secure); return this; }
    public ObjectBuilder matSecureField(int secure) { stream.matSecureField(secure); return this; }
    public ObjectBuilder matAutoComplete(int auto) { stream.matAutoComplete(auto); return this; }

    // --- Controls ---
    public ObjectBuilder matTreectrlSetClass(int cls) { stream.matTreectrlSetClass(cls); return this; }
    public ObjectBuilder matCtrlAttribute(int attr) { stream.matCtrlAttribute(attr); return this; }
    public ObjectBuilder matNavarrowPos(int pos) { stream.matNavarrowPos(pos); return this; }
    public ObjectBuilder matNavarrowPosX(int x) { stream.matNavarrowPosX(x); return this; }
    public ObjectBuilder matNavarrowPosY(int y) { stream.matNavarrowPosY(y); return this; }
    public ObjectBuilder matTextOnPicturePos(int pos) { stream.matTextOnPicturePos(pos); return this; }
    public ObjectBuilder matThumbSize(int size) { stream.matThumbSize(size); return this; }
    public ObjectBuilder matTabSetCurSel(int sel) { stream.matTabSetCurSel(sel); return this; }
    public ObjectBuilder matTabGetCurSel() { stream.matTabGetCurSel(); return this; }
    public ObjectBuilder matPopupRelativeId(int id) { stream.matPopupRelativeId(id); return this; }
    public ObjectBuilder matPopupPfcPath(String path) { stream.matPopupPfcPath(path); return this; }

    // ========== Typed Boolean MAT Methods (no-arg = true) ==========

    public ObjectBuilder matBoolVerticalScroll() { stream.matBoolVerticalScroll(); return this; }
    public ObjectBuilder matBoolHorizontalScroll() { stream.matBoolHorizontalScroll(); return this; }
    public ObjectBuilder matBoolDisabled() { stream.matBoolDisabled(); return this; }
    public ObjectBuilder matBoolDefault() { stream.matBoolDefault(); return this; }
    public ObjectBuilder matBoolResizeVertical() { stream.matBoolResizeVertical(); return this; }
    public ObjectBuilder matBoolResizeHorizontal() { stream.matBoolResizeHorizontal(); return this; }
    public ObjectBuilder matBoolModal() { stream.matBoolModal(); return this; }
    public ObjectBuilder matBoolInvisible() { stream.matBoolInvisible(); return this; }
    public ObjectBuilder matBoolHidden() { stream.matBoolHidden(); return this; }
    public ObjectBuilder matBoolListIcons() { stream.matBoolListIcons(); return this; }
    public ObjectBuilder matBoolGraphicView() { stream.matBoolGraphicView(); return this; }
    public ObjectBuilder matBoolPalette() { stream.matBoolPalette(); return this; }
    public ObjectBuilder matBoolForceScroll() { stream.matBoolForceScroll(); return this; }
    public ObjectBuilder matBoolProtectedInput() { stream.matBoolProtectedInput(); return this; }
    public ObjectBuilder matBoolForceNoScroll() { stream.matBoolForceNoScroll(); return this; }
    public ObjectBuilder matBoolNonCloseable() { stream.matBoolNonCloseable(); return this; }
    public ObjectBuilder matBoolExportable() { stream.matBoolExportable(); return this; }
    public ObjectBuilder matBoolImportable() { stream.matBoolImportable(); return this; }
    public ObjectBuilder matBoolWriteable() { stream.matBoolWriteable(); return this; }
    public ObjectBuilder matBoolFloating() { stream.matBoolFloating(); return this; }
    public ObjectBuilder matBoolContiguous() { stream.matBoolContiguous(); return this; }
    public ObjectBuilder matBoolMenu() { stream.matBoolMenu(); return this; }
    public ObjectBuilder matBoolDefaultSend() { stream.matBoolDefaultSend(); return this; }
    public ObjectBuilder matBoolDoubleSpace() { stream.matBoolDoubleSpace(); return this; }
    public ObjectBuilder matBoolInvert() { stream.matBoolInvert(); return this; }
    public ObjectBuilder matBoolPermanent() { stream.matBoolPermanent(); return this; }
    public ObjectBuilder matBoolIgnore() { stream.matBoolIgnore(); return this; }
    public ObjectBuilder matBoolNoBorder() { stream.matBoolNoBorder(); return this; }
    public ObjectBuilder matBoolModified() { stream.matBoolModified(); return this; }
    public ObjectBuilder matBoolPrecise() { stream.matBoolPrecise(); return this; }
    public ObjectBuilder matBoolGradualShadow() { stream.matBoolGradualShadow(); return this; }
    public ObjectBuilder matBoolBackgroundPic() { stream.matBoolBackgroundPic(); return this; }
    public ObjectBuilder matBoolBackgroundFlood() { stream.matBoolBackgroundFlood(); return this; }
    public ObjectBuilder matBoolRepeatAnimation() { stream.matBoolRepeatAnimation(); return this; }
    public ObjectBuilder matBoolBackgroundTile() { stream.matBoolBackgroundTile(); return this; }
    public ObjectBuilder matBoolFirstScript() { stream.matBoolFirstScript(); return this; }
    public ObjectBuilder matBoolListAllowEntry() { stream.matBoolListAllowEntry(); return this; }
    public ObjectBuilder matBoolExpandToFit() { stream.matBoolExpandToFit(); return this; }
    public ObjectBuilder matBoolSane() { stream.matBoolSane(); return this; }
    public ObjectBuilder matBoolUrlSink() { stream.matBoolUrlSink(); return this; }
    public ObjectBuilder matBoolDropAtTop() { stream.matBoolDropAtTop(); return this; }
    public ObjectBuilder matBoolLanguagePopup() { stream.matBoolLanguagePopup(); return this; }
    public ObjectBuilder matBoolPaletteArt() { stream.matBoolPaletteArt(); return this; }
    public ObjectBuilder matBoolToolGroup() { stream.matBoolToolGroup(); return this; }
    public ObjectBuilder matBoolSmallIcon() { stream.matBoolSmallIcon(); return this; }
    public ObjectBuilder matBoolEncodeUnicode() { stream.matBoolEncodeUnicode(); return this; }
    public ObjectBuilder matBoolPageControl() { stream.matBoolPageControl(); return this; }
    public ObjectBuilder matBoolSpinner() { stream.matBoolSpinner(); return this; }
    public ObjectBuilder matBoolDetached() { stream.matBoolDetached(); return this; }
    public ObjectBuilder matBoolCustomizable() { stream.matBoolCustomizable(); return this; }
    public ObjectBuilder matBoolDetachable() { stream.matBoolDetachable(); return this; }
    public ObjectBuilder matBoolDockHorizontal() { stream.matBoolDockHorizontal(); return this; }
    public ObjectBuilder matBoolDockVertical() { stream.matBoolDockVertical(); return this; }
    public ObjectBuilder matBoolChildrenRemovable() { stream.matBoolChildrenRemovable(); return this; }
    public ObjectBuilder matBoolChildrenMovable() { stream.matBoolChildrenMovable(); return this; }
    public ObjectBuilder matBoolChildRemovable() { stream.matBoolChildRemovable(); return this; }
    public ObjectBuilder matBoolChildMovable() { stream.matBoolChildMovable(); return this; }
    public ObjectBuilder matBoolChildLineFeed() { stream.matBoolChildLineFeed(); return this; }
    public ObjectBuilder matBoolSharedStyle() { stream.matBoolSharedStyle(); return this; }
    public ObjectBuilder matBoolActiveOffline() { stream.matBoolActiveOffline(); return this; }
    public ObjectBuilder matBoolActiveOnline() { stream.matBoolActiveOnline(); return this; }
    public ObjectBuilder matBoolInactiveForGuest() { stream.matBoolInactiveForGuest(); return this; }
    public ObjectBuilder matBoolDropdownButton() { stream.matBoolDropdownButton(); return this; }
    public ObjectBuilder matBoolPopupMenu() { stream.matBoolPopupMenu(); return this; }
    public ObjectBuilder matBoolIgnoreUrlList() { stream.matBoolIgnoreUrlList(); return this; }
    public ObjectBuilder matBoolDrawFocus() { stream.matBoolDrawFocus(); return this; }
    public ObjectBuilder matBoolLoggable() { stream.matBoolLoggable(); return this; }
    public ObjectBuilder matBoolHideUrl() { stream.matBoolHideUrl(); return this; }
    public ObjectBuilder matBoolAutoCloseable() { stream.matBoolAutoCloseable(); return this; }
    public ObjectBuilder matBoolSignoffMenu() { stream.matBoolSignoffMenu(); return this; }
    public ObjectBuilder matBoolIgnoreUrl() { stream.matBoolIgnoreUrl(); return this; }
    public ObjectBuilder matBoolSavableToPfc() { stream.matBoolSavableToPfc(); return this; }
    public ObjectBuilder matBoolStaticUrl() { stream.matBoolStaticUrl(); return this; }
    public ObjectBuilder matBoolAllowTabbing() { stream.matBoolAllowTabbing(); return this; }
    public ObjectBuilder matBoolDefaultTrigger() { stream.matBoolDefaultTrigger(); return this; }
    public ObjectBuilder matBoolHideTicks() { stream.matBoolHideTicks(); return this; }
    public ObjectBuilder matBoolIsSlider() { stream.matBoolIsSlider(); return this; }

    // ========== Typed Boolean MAT Methods (with boolean arg) ==========

    public ObjectBuilder matBoolVerticalScroll(boolean enabled) { stream.matBoolVerticalScroll(enabled); return this; }
    public ObjectBuilder matBoolHorizontalScroll(boolean enabled) { stream.matBoolHorizontalScroll(enabled); return this; }
    public ObjectBuilder matBoolDisabled(boolean enabled) { stream.matBoolDisabled(enabled); return this; }
    public ObjectBuilder matBoolDefault(boolean enabled) { stream.matBoolDefault(enabled); return this; }
    public ObjectBuilder matBoolResizeVertical(boolean enabled) { stream.matBoolResizeVertical(enabled); return this; }
    public ObjectBuilder matBoolResizeHorizontal(boolean enabled) { stream.matBoolResizeHorizontal(enabled); return this; }
    public ObjectBuilder matBoolModal(boolean enabled) { stream.matBoolModal(enabled); return this; }
    public ObjectBuilder matBoolInvisible(boolean enabled) { stream.matBoolInvisible(enabled); return this; }
    public ObjectBuilder matBoolHidden(boolean enabled) { stream.matBoolHidden(enabled); return this; }
    public ObjectBuilder matBoolListIcons(boolean enabled) { stream.matBoolListIcons(enabled); return this; }
    public ObjectBuilder matBoolGraphicView(boolean enabled) { stream.matBoolGraphicView(enabled); return this; }
    public ObjectBuilder matBoolPalette(boolean enabled) { stream.matBoolPalette(enabled); return this; }
    public ObjectBuilder matBoolForceScroll(boolean enabled) { stream.matBoolForceScroll(enabled); return this; }
    public ObjectBuilder matBoolProtectedInput(boolean enabled) { stream.matBoolProtectedInput(enabled); return this; }
    public ObjectBuilder matBoolForceNoScroll(boolean enabled) { stream.matBoolForceNoScroll(enabled); return this; }
    public ObjectBuilder matBoolNonCloseable(boolean enabled) { stream.matBoolNonCloseable(enabled); return this; }
    public ObjectBuilder matBoolExportable(boolean enabled) { stream.matBoolExportable(enabled); return this; }
    public ObjectBuilder matBoolImportable(boolean enabled) { stream.matBoolImportable(enabled); return this; }
    public ObjectBuilder matBoolWriteable(boolean enabled) { stream.matBoolWriteable(enabled); return this; }
    public ObjectBuilder matBoolFloating(boolean enabled) { stream.matBoolFloating(enabled); return this; }
    public ObjectBuilder matBoolContiguous(boolean enabled) { stream.matBoolContiguous(enabled); return this; }
    public ObjectBuilder matBoolMenu(boolean enabled) { stream.matBoolMenu(enabled); return this; }
    public ObjectBuilder matBoolDefaultSend(boolean enabled) { stream.matBoolDefaultSend(enabled); return this; }
    public ObjectBuilder matBoolDoubleSpace(boolean enabled) { stream.matBoolDoubleSpace(enabled); return this; }
    public ObjectBuilder matBoolInvert(boolean enabled) { stream.matBoolInvert(enabled); return this; }
    public ObjectBuilder matBoolPermanent(boolean enabled) { stream.matBoolPermanent(enabled); return this; }
    public ObjectBuilder matBoolIgnore(boolean enabled) { stream.matBoolIgnore(enabled); return this; }
    public ObjectBuilder matBoolNoBorder(boolean enabled) { stream.matBoolNoBorder(enabled); return this; }
    public ObjectBuilder matBoolModified(boolean enabled) { stream.matBoolModified(enabled); return this; }
    public ObjectBuilder matBoolPrecise(boolean enabled) { stream.matBoolPrecise(enabled); return this; }
    public ObjectBuilder matBoolGradualShadow(boolean enabled) { stream.matBoolGradualShadow(enabled); return this; }
    public ObjectBuilder matBoolBackgroundPic(boolean enabled) { stream.matBoolBackgroundPic(enabled); return this; }
    public ObjectBuilder matBoolBackgroundFlood(boolean enabled) { stream.matBoolBackgroundFlood(enabled); return this; }
    public ObjectBuilder matBoolRepeatAnimation(boolean enabled) { stream.matBoolRepeatAnimation(enabled); return this; }
    public ObjectBuilder matBoolBackgroundTile(boolean enabled) { stream.matBoolBackgroundTile(enabled); return this; }
    public ObjectBuilder matBoolFirstScript(boolean enabled) { stream.matBoolFirstScript(enabled); return this; }
    public ObjectBuilder matBoolListAllowEntry(boolean enabled) { stream.matBoolListAllowEntry(enabled); return this; }
    public ObjectBuilder matBoolExpandToFit(boolean enabled) { stream.matBoolExpandToFit(enabled); return this; }
    public ObjectBuilder matBoolSane(boolean enabled) { stream.matBoolSane(enabled); return this; }
    public ObjectBuilder matBoolUrlSink(boolean enabled) { stream.matBoolUrlSink(enabled); return this; }
    public ObjectBuilder matBoolDropAtTop(boolean enabled) { stream.matBoolDropAtTop(enabled); return this; }
    public ObjectBuilder matBoolLanguagePopup(boolean enabled) { stream.matBoolLanguagePopup(enabled); return this; }
    public ObjectBuilder matBoolPaletteArt(boolean enabled) { stream.matBoolPaletteArt(enabled); return this; }
    public ObjectBuilder matBoolToolGroup(boolean enabled) { stream.matBoolToolGroup(enabled); return this; }
    public ObjectBuilder matBoolSmallIcon(boolean enabled) { stream.matBoolSmallIcon(enabled); return this; }
    public ObjectBuilder matBoolEncodeUnicode(boolean enabled) { stream.matBoolEncodeUnicode(enabled); return this; }
    public ObjectBuilder matBoolPageControl(boolean enabled) { stream.matBoolPageControl(enabled); return this; }
    public ObjectBuilder matBoolSpinner(boolean enabled) { stream.matBoolSpinner(enabled); return this; }
    public ObjectBuilder matBoolDetached(boolean enabled) { stream.matBoolDetached(enabled); return this; }
    public ObjectBuilder matBoolCustomizable(boolean enabled) { stream.matBoolCustomizable(enabled); return this; }
    public ObjectBuilder matBoolDetachable(boolean enabled) { stream.matBoolDetachable(enabled); return this; }
    public ObjectBuilder matBoolDockHorizontal(boolean enabled) { stream.matBoolDockHorizontal(enabled); return this; }
    public ObjectBuilder matBoolDockVertical(boolean enabled) { stream.matBoolDockVertical(enabled); return this; }
    public ObjectBuilder matBoolChildrenRemovable(boolean enabled) { stream.matBoolChildrenRemovable(enabled); return this; }
    public ObjectBuilder matBoolChildrenMovable(boolean enabled) { stream.matBoolChildrenMovable(enabled); return this; }
    public ObjectBuilder matBoolChildRemovable(boolean enabled) { stream.matBoolChildRemovable(enabled); return this; }
    public ObjectBuilder matBoolChildMovable(boolean enabled) { stream.matBoolChildMovable(enabled); return this; }
    public ObjectBuilder matBoolChildLineFeed(boolean enabled) { stream.matBoolChildLineFeed(enabled); return this; }
    public ObjectBuilder matBoolSharedStyle(boolean enabled) { stream.matBoolSharedStyle(enabled); return this; }
    public ObjectBuilder matBoolActiveOffline(boolean enabled) { stream.matBoolActiveOffline(enabled); return this; }
    public ObjectBuilder matBoolActiveOnline(boolean enabled) { stream.matBoolActiveOnline(enabled); return this; }
    public ObjectBuilder matBoolInactiveForGuest(boolean enabled) { stream.matBoolInactiveForGuest(enabled); return this; }
    public ObjectBuilder matBoolDropdownButton(boolean enabled) { stream.matBoolDropdownButton(enabled); return this; }
    public ObjectBuilder matBoolPopupMenu(boolean enabled) { stream.matBoolPopupMenu(enabled); return this; }
    public ObjectBuilder matBoolIgnoreUrlList(boolean enabled) { stream.matBoolIgnoreUrlList(enabled); return this; }
    public ObjectBuilder matBoolDrawFocus(boolean enabled) { stream.matBoolDrawFocus(enabled); return this; }
    public ObjectBuilder matBoolLoggable(boolean enabled) { stream.matBoolLoggable(enabled); return this; }
    public ObjectBuilder matBoolHideUrl(boolean enabled) { stream.matBoolHideUrl(enabled); return this; }
    public ObjectBuilder matBoolAutoCloseable(boolean enabled) { stream.matBoolAutoCloseable(enabled); return this; }
    public ObjectBuilder matBoolSignoffMenu(boolean enabled) { stream.matBoolSignoffMenu(enabled); return this; }
    public ObjectBuilder matBoolIgnoreUrl(boolean enabled) { stream.matBoolIgnoreUrl(enabled); return this; }
    public ObjectBuilder matBoolSavableToPfc(boolean enabled) { stream.matBoolSavableToPfc(enabled); return this; }
    public ObjectBuilder matBoolStaticUrl(boolean enabled) { stream.matBoolStaticUrl(enabled); return this; }
    public ObjectBuilder matBoolAllowTabbing(boolean enabled) { stream.matBoolAllowTabbing(enabled); return this; }
    public ObjectBuilder matBoolDefaultTrigger(boolean enabled) { stream.matBoolDefaultTrigger(enabled); return this; }
    public ObjectBuilder matBoolHideTicks(boolean enabled) { stream.matBoolHideTicks(enabled); return this; }
    public ObjectBuilder matBoolIsSlider(boolean enabled) { stream.matBoolIsSlider(enabled); return this; }

    // ========== Typed MAN Methods ==========

    public ObjectBuilder manUpdateDisplay() { stream.manUpdateDisplay(); return this; }
    public ObjectBuilder manUpdateEndObject() { stream.manUpdateEndObject(); return this; }
    public ObjectBuilder manForceUpdate() { stream.manForceUpdate(); return this; }
    public ObjectBuilder manClearObject() { stream.manClearObject(); return this; }
    public ObjectBuilder manClearRelative(int offset) { stream.manClearRelative(offset); return this; }
    public ObjectBuilder manMakeFocus() { stream.manMakeFocus(); return this; }
    public ObjectBuilder manClose() { stream.manClose(); return this; }
    public ObjectBuilder manClose(FdoGid gid) { stream.manClose(gid); return this; }
    public ObjectBuilder manCloseChildren() { stream.manCloseChildren(); return this; }
    public ObjectBuilder manCloseUpdate() { stream.manCloseUpdate(); return this; }
    public ObjectBuilder manAppendData(String data) { stream.manAppendData(data); return this; }
    public ObjectBuilder manAppendData(byte[] data) { stream.manAppendData(data); return this; }
    public ObjectBuilder manReplaceData(String data) { stream.manReplaceData(data); return this; }
    public ObjectBuilder manEndData() { stream.manEndData(); return this; }
    public ObjectBuilder manPresetGid(FdoGid gid) { stream.manPresetGid(gid); return this; }
    public ObjectBuilder manPresetTitle(String title) { stream.manPresetTitle(title); return this; }
    public ObjectBuilder manPresetUrl(String url) { stream.manPresetUrl(url); return this; }
    public ObjectBuilder manPresetRelative(int offset) { stream.manPresetRelative(offset); return this; }
    public ObjectBuilder manPresetTag(String tag) { stream.manPresetTag(tag); return this; }
    public ObjectBuilder manSetResponseId(int responseId) { stream.manSetResponseId(responseId); return this; }
    public ObjectBuilder manResponsePop() { stream.manResponsePop(); return this; }
    public ObjectBuilder manIgnoreResponse() { stream.manIgnoreResponse(); return this; }
    public ObjectBuilder manDoMagicTokenArg(String token) { stream.manDoMagicTokenArg(token); return this; }
    public ObjectBuilder manUseDefaultTitle() { stream.manUseDefaultTitle(); return this; }
    public ObjectBuilder manSetDefaultTitle() { stream.manSetDefaultTitle(); return this; }
    public ObjectBuilder manEnableOneShotTimer(int duration) { stream.manEnableOneShotTimer(duration); return this; }
    public ObjectBuilder manEnableContinuousTimer(int duration) { stream.manEnableContinuousTimer(duration); return this; }
    public ObjectBuilder manKillTimer() { stream.manKillTimer(); return this; }
    public ObjectBuilder manGetChildCount() { stream.manGetChildCount(); return this; }
    public ObjectBuilder manSortItems() { stream.manSortItems(); return this; }
    public ObjectBuilder manDisplayPopupMenu() { stream.manDisplayPopupMenu(); return this; }
    public ObjectBuilder manSpellCheck() { stream.manSpellCheck(); return this; }
    public ObjectBuilder manPlaceCursor(int position) { stream.manPlaceCursor(position); return this; }
    public ObjectBuilder manSetEditPosition(int position) { stream.manSetEditPosition(position); return this; }
    public ObjectBuilder manSetEditPositionToEnd() { stream.manSetEditPositionToEnd(); return this; }

    // ========== Action Methods ==========

    /**
     * Set an action for the "select" criterion.
     *
     * @param actionBuilder Consumer that builds the action stream
     * @return this builder for chaining
     */
    public ObjectBuilder onSelect(Consumer<StreamBuilder> actionBuilder) {
        stream.setAction(Criterion.SELECT, actionBuilder);
        return this;
    }

    /**
     * Set an action for the "gain_focus" criterion.
     *
     * @param actionBuilder Consumer that builds the action stream
     * @return this builder for chaining
     */
    public ObjectBuilder onGainFocus(Consumer<StreamBuilder> actionBuilder) {
        stream.setAction(Criterion.GAIN_FOCUS, actionBuilder);
        return this;
    }

    // ========== Nested Object Methods (legacy API) ==========

    /**
     * Start a child object within this object.
     *
     * @param type The child object type
     * @return An ObjectBuilder for the child object
     * @deprecated Use {@link #object(ObjectType, Consumer)} for automatic scoping instead.
     */
    @Deprecated
    public ObjectBuilder startObject(ObjectType type) {
        return stream.startObject(type);
    }

    /**
     * Start a child object with a title.
     *
     * @param type The child object type
     * @param title The child object title
     * @return An ObjectBuilder for the child object
     * @deprecated Use {@link #object(ObjectType, String, Consumer)} for automatic scoping instead.
     */
    @Deprecated
    public ObjectBuilder startObject(ObjectType type, String title) {
        return stream.startObject(type, title);
    }

    /**
     * Start a sibling object.
     *
     * @param type The sibling object type
     * @return An ObjectBuilder for the sibling object
     * @deprecated Use {@link #sibling(ObjectType, Consumer)} for automatic scoping instead.
     */
    @Deprecated
    public ObjectBuilder startSibling(ObjectType type) {
        return stream.startSibling(type);
    }

    /**
     * Start a sibling object with a title.
     *
     * @param type The sibling object type
     * @param title The sibling object title
     * @return An ObjectBuilder for the sibling object
     * @deprecated Use {@link #sibling(ObjectType, String, Consumer)} for automatic scoping instead.
     */
    @Deprecated
    public ObjectBuilder startSibling(ObjectType type, String title) {
        return stream.startSibling(type, title);
    }

    // ========== Scoped Nested Object Methods (recommended) ==========

    /**
     * Build a child object with automatic scoping - no endObject() needed.
     *
     * <p>Example:
     * <pre>{@code
     * parent.object(ObjectType.TRIGGER, "Button", btn -> {
     *     btn.triggerStyle(TriggerStyle.FRAMED);
     *     btn.onSelect(a -> a.sendTokenArg("LP"));
     * });
     * }</pre>
     *
     * @param type The child object type
     * @param ops Consumer that configures the child object
     * @return this builder for chaining
     */
    public ObjectBuilder object(ObjectType type, Consumer<ObjectBuilder> ops) {
        stream.object(type, ops);
        return this;
    }

    /**
     * Build a child object with title and automatic scoping.
     *
     * @param type The child object type
     * @param title The child object title
     * @param ops Consumer that configures the child object
     * @return this builder for chaining
     */
    public ObjectBuilder object(ObjectType type, String title, Consumer<ObjectBuilder> ops) {
        stream.object(type, title, ops);
        return this;
    }

    /**
     * Build a sibling object with automatic scoping.
     *
     * <p>Emits man_start_sibling (which implicitly ends the current object)
     * and man_end_object at the end.
     *
     * <p>Example:
     * <pre>{@code
     * toolbar.object(ObjectType.TRIGGER, "First", btn -> {
     *     btn.artId(ART_1);
     * });
     * toolbar.sibling(ObjectType.TRIGGER, "Second", btn -> {
     *     btn.artId(ART_2);
     * });
     * }</pre>
     *
     * @param type The sibling object type
     * @param ops Consumer that configures the sibling object
     * @return this builder for chaining
     */
    public ObjectBuilder sibling(ObjectType type, Consumer<ObjectBuilder> ops) {
        return sibling(type, null, ops);
    }

    /**
     * Build a sibling object with title and automatic scoping.
     *
     * @param type The sibling object type
     * @param title The sibling object title
     * @param ops Consumer that configures the sibling object
     * @return this builder for chaining
     */
    public ObjectBuilder sibling(ObjectType type, String title, Consumer<ObjectBuilder> ops) {
        // man_start_sibling implicitly ends the current object and starts a new one
        stream.atom(ManAtom.START_SIBLING, type, title != null ? title : "");
        try {
            ops.accept(this);
        } finally {
            stream.atom(ManAtom.END_OBJECT);
        }
        return this;
    }

    // ========== ACT Action Methods ==========

    public ObjectBuilder act(ActAtom atom, Object... args) { stream.act(atom, args); return this; }
    public ObjectBuilder actSetCriterion(Criterion criterion) { stream.actSetCriterion(criterion); return this; }
    public ObjectBuilder actSetCriterion(CriterionArg criterion) { stream.actSetCriterion(criterion); return this; }
    public ObjectBuilder actDoAction(Criterion criterion) { stream.actDoAction(criterion); return this; }
    public ObjectBuilder actSetCriterion(int criterion) { stream.actSetCriterion(criterion); return this; }
    public ObjectBuilder actDoAction(String criterion) { stream.actDoAction(criterion); return this; }
    public ObjectBuilder actReplaceAction(Consumer<StreamBuilder> actionBuilder) { stream.actReplaceAction(actionBuilder); return this; }
    public ObjectBuilder actReplaceSelectAction(Consumer<StreamBuilder> actionBuilder) { stream.actReplaceSelectAction(actionBuilder); return this; }
    public ObjectBuilder actSetInheritance(int inheritance) { stream.actSetInheritance(inheritance); return this; }
    public ObjectBuilder actSoundBeep() { stream.actSoundBeep(); return this; }
    public ObjectBuilder actModifyAction(Consumer<StreamBuilder> actionBuilder) { stream.actModifyAction(actionBuilder); return this; }
    public ObjectBuilder actSetTestIndex(int index) { stream.actSetTestIndex(index); return this; }
    public ObjectBuilder actClrTestIndex() { stream.actClrTestIndex(); return this; }
    public ObjectBuilder actSetIndex(int index) { stream.actSetIndex(index); return this; }
    public ObjectBuilder actAppendAction(Consumer<StreamBuilder> actionBuilder) { stream.actAppendAction(actionBuilder); return this; }
    public ObjectBuilder actPrependAction(Consumer<StreamBuilder> actionBuilder) { stream.actPrependAction(actionBuilder); return this; }
    public ObjectBuilder actChangeAction(Consumer<StreamBuilder> actionBuilder) { stream.actChangeAction(actionBuilder); return this; }
    public ObjectBuilder actAppendSelectAction(Consumer<StreamBuilder> actionBuilder) { stream.actAppendSelectAction(actionBuilder); return this; }
    public ObjectBuilder actPrependSelectAction(Consumer<StreamBuilder> actionBuilder) { stream.actPrependSelectAction(actionBuilder); return this; }
    public ObjectBuilder actChangeSelectAction(Consumer<StreamBuilder> actionBuilder) { stream.actChangeSelectAction(actionBuilder); return this; }
    public ObjectBuilder actCopyActionToReg(int reg) { stream.actCopyActionToReg(reg); return this; }
    public ObjectBuilder actReplaceActionFromReg(int reg) { stream.actReplaceActionFromReg(reg); return this; }
    public ObjectBuilder actAppendActionFromReg(int reg) { stream.actAppendActionFromReg(reg); return this; }
    public ObjectBuilder actPrependActionFromReg(int reg) { stream.actPrependActionFromReg(reg); return this; }
    public ObjectBuilder actChangeActionFromReg(int reg) { stream.actChangeActionFromReg(reg); return this; }
    public ObjectBuilder actSetActionInReg(int reg) { stream.actSetActionInReg(reg); return this; }
    public ObjectBuilder actInterpretPacket(byte[] packet) { stream.actInterpretPacket(packet); return this; }
    public ObjectBuilder actSetDbLength(int length) { stream.actSetDbLength(length); return this; }
    public ObjectBuilder actGetDbRecord() { stream.actGetDbRecord(); return this; }
    public ObjectBuilder actSetDbId(int id) { stream.actSetDbId(id); return this; }
    public ObjectBuilder actSetDbRecord(byte[] record) { stream.actSetDbRecord(record); return this; }
    public ObjectBuilder actSetGuestFlag(int flag) { stream.actSetGuestFlag(flag); return this; }
    public ObjectBuilder actSetNewuserFlag(int flag) { stream.actSetNewuserFlag(flag); return this; }
    public ObjectBuilder actSetDbOffset(int offset) { stream.actSetDbOffset(offset); return this; }
    public ObjectBuilder actGetDbValue() { stream.actGetDbValue(); return this; }
    public ObjectBuilder actFormatQuote(String quote) { stream.actFormatQuote(quote); return this; }
    public ObjectBuilder actReplacePopupMenuAction(Consumer<StreamBuilder> actionBuilder) { stream.actReplacePopupMenuAction(actionBuilder); return this; }

    // ========== Scoped Context Methods ==========

    /**
     * Build within a relative context with automatic scoping - no endContext() needed.
     *
     * @param offset The relative offset to set context to
     * @param ops Operations to perform within the context
     * @return this builder for chaining
     */
    public ObjectBuilder context(int offset, Consumer<StreamBuilder> ops) {
        stream.context(offset, ops);
        return this;
    }

    /**
     * Build within a global ID context with automatic scoping.
     *
     * @param gid The global ID to set context to
     * @param ops Operations to perform within the context
     * @return this builder for chaining
     */
    public ObjectBuilder contextGlobalId(FdoGid gid, Consumer<StreamBuilder> ops) {
        stream.contextGlobalId(gid, ops);
        return this;
    }

    // ========== End Object (legacy API) ==========

    /**
     * End this object and return to the parent stream.
     *
     * @return The parent StreamBuilder
     * @deprecated Use scoped object() methods instead which handle end automatically.
     */
    @Deprecated
    public StreamBuilder endObject() {
        return stream.endObject();
    }

    /**
     * Get the underlying stream builder.
     *
     * @return The StreamBuilder this ObjectBuilder wraps
     */
    public StreamBuilder stream() {
        return stream;
    }
}
