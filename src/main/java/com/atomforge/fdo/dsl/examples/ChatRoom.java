package com.atomforge.fdo.dsl.examples;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.FdoScript;
import com.atomforge.fdo.dsl.StreamBuilder;
import com.atomforge.fdo.dsl.atoms.*;
import com.atomforge.fdo.dsl.values.*;
import com.atomforge.fdo.model.FdoGid;

/**
 * Example: Chat Room window with toolbar, user list, chat view, and input area.
 *
 * This demonstrates a complex nested FDO structure including:
 * - Deep object nesting (org_group, triggers, lists, views)
 * - Complex action handlers with variable manipulation
 * - Multiple protocols (MAT, MAN, UNI, SM, DE, VAR, BUF, ACT, IF, P3)
 * - Sibling object chains
 * - Data extraction and validation
 *
 * This replicates a classic chat room UI pattern.
 */
public final class ChatRoom {

    // Art IDs for toolbar buttons
    private static final FdoGid ART_LIST_ROOMS = FdoGid.of(1, 69, 1329);
    private static final FdoGid ART_CENTER_STAGE = FdoGid.of(1, 69, 1335);
    private static final FdoGid ART_PC_STUDIO = FdoGid.of(1, 69, 1332);
    private static final FdoGid ART_PREFERENCES = FdoGid.of(1, 69, 1301);
    private static final FdoGid ART_BACKGROUND = FdoGid.of(1, 69, 27256);

    // Style and form GIDs
    private static final FdoGid STYLE_ID = FdoGid.of(32, 117);
    private static final FdoGid PREFERENCES_FORM = FdoGid.of(32, 245);
    private static final FdoGid PROFILE_FORM = FdoGid.of(32, 168);
    private static final FdoGid CLOSE_CONTEXT = FdoGid.of(19, 0, 0);

    // Navigation targets
    private static final FdoGid CENTER_STAGE_TARGET = FdoGid.of(8, 62);
    private static final FdoGid PC_STUDIO_TARGET = FdoGid.of(8, 20);

    // Relative tags for elements
    private static final int TAG_CHAT_VIEW = 256;
    private static final int TAG_USER_LIST = 257;
    private static final int TAG_INPUT_FIELD = 258;
    private static final int TAG_PC_STUDIO = 259;
    private static final int TAG_CENTER_STAGE = 260;
    private static final int TAG_PEOPLE_COUNT = 261;

    // Button colors (template placeholders - will use raw hex)
    private String buttonColorFace = "C0C0C0";
    private String buttonColorText = "000000";
    private String buttonColorTopEdge = "FFFFFF";
    private String buttonColorBottomEdge = "808080";

    public ChatRoom() {
    }

    /**
     * Set button face color (hex RGB).
     */
    public ChatRoom buttonColorFace(String color) {
        this.buttonColorFace = color;
        return this;
    }

    /**
     * Set button text color (hex RGB).
     */
    public ChatRoom buttonColorText(String color) {
        this.buttonColorText = color;
        return this;
    }

    /**
     * Compile this ChatRoom to FDO binary format.
     */
    public byte[] compile() throws FdoException {
        StreamBuilder stream = FdoScript.stream();

        // Start stream
        stream.uni(UniAtom.START_STREAM);

        // Main window container
        stream.startObject(ObjectType.IND_GROUP, "")
            .orientation(Orientation.VFF)
            .position(Position.CENTER_CENTER)
            .mat(MatAtom.STYLE_ID, STYLE_ID)
            .mat(MatAtom.BOOL_BACKGROUND_PIC, 0)  // no
            .backgroundTile()
            .artId(ART_BACKGROUND);

        // Outer org_group
        stream.startObject(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.VFF);

        // Row container
        stream.startObject(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.HEF);

        // Toolbar container (left side)
        stream.startObject(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.HLE);

        // List Rooms button
        stream.startObject(ObjectType.TRIGGER, "List Rooms")
            .mat(MatAtom.TITLE_WIDTH, 8)
            .artId(ART_LIST_ROOMS);
        addListRoomsAction(stream);

        // Center Stage button (sibling)
        stream.startSibling(ObjectType.TRIGGER, "Center Stage")
            .mat(MatAtom.TITLE_WIDTH, 8)
            .mat(MatAtom.RELATIVE_TAG, TAG_CENTER_STAGE)
            .artId(ART_CENTER_STAGE);
        addCenterStageAction(stream);

        // PC Studio button (sibling)
        stream.startSibling(ObjectType.TRIGGER, "PC Studio")
            .mat(MatAtom.TITLE_WIDTH, 8)
            .mat(MatAtom.RELATIVE_TAG, TAG_PC_STUDIO)
            .artId(ART_PC_STUDIO);
        addPcStudioAction(stream);

        // Preferences button (sibling)
        stream.startSibling(ObjectType.TRIGGER, "Preferences")
            .mat(MatAtom.TITLE_WIDTH, 8)
            .artId(ART_PREFERENCES);
        addPreferencesAction(stream);

        stream.endObject();  // End Preferences trigger

        // People count section (sibling to toolbar)
        stream.startSibling(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.VCT);

        // "People Here:" label
        stream.startObject(ObjectType.ORNAMENT, "")
            .size(12, 1, 12)
            .fontSize(9)
            .fontStyle(FontStyle.BOLD)
            .appendData("People Here:");

        // Count ornament (sibling)
        stream.startSibling(ObjectType.ORNAMENT, "")
            .size(2, 1, 2)
            .mat(MatAtom.RELATIVE_TAG, TAG_PEOPLE_COUNT)
            .fontSize(9);

        stream.endObject();  // End count ornament

        // User list (sibling to people count section)
        stream.startSibling(ObjectType.DSS_LIST, "")
            .orientation(Orientation.VFF)
            .size(12, 5)
            .mat(MatAtom.RELATIVE_TAG, TAG_USER_LIST)
            .titlePos(TitlePosition.LEFT_TOP_OR_LEFT)
            .mat(MatAtom.BOOL_RESIZE_HORIZONTAL, 0)  // no
            .mat(MatAtom.BOOL_DEFAULT_SEND, 0)  // no
            .fontId(FontId.ARIAL)
            .fontSize(9);

        // User list select action (complex profile lookup)
        addUserListSelectAction(stream);

        stream.endObject();  // End user list

        // Second select action on user list
        addUserListSecondAction(stream);

        // Chat area (sibling to user list)
        stream.startSibling(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.VLT);

        // Chat view
        stream.startObject(ObjectType.VIEW, "")
            .size(67, 11, 8192)
            .mat(MatAtom.RELATIVE_TAG, TAG_CHAT_VIEW)
            .mat(MatAtom.RULER, 10)
            .writeable(true)
            .mat(MatAtom.SCROLL_THRESHOLD, 4096)
            .mat(MatAtom.BOOL_FORCE_SCROLL)
            .mat(MatAtom.PARAGRAPH, 1)  // yes
            .logType(LogType.CHAT_LOG);

        stream.endObject();  // End chat view

        // Chat area select action
        addChatAreaSelectAction(stream);

        // Input area (child of chat area org_group)
        stream.startObject(ObjectType.ORG_GROUP, "")
            .orientation(Orientation.HLF);

        // Input field
        stream.startObject(ObjectType.EDIT_VIEW, "")
            .size(61, 1, 92)
            .mat(MatAtom.RELATIVE_TAG, TAG_INPUT_FIELD)
            .titlePos(TitlePosition.LEFT_CENTER)
            .mat(MatAtom.BOOL_VERTICAL_SCROLL, 0)  // no
            .mat(MatAtom.BOOL_RESIZE_VERTICAL, 0)  // no
            .mat(MatAtom.BOOL_RESIZE_HORIZONTAL, 0)  // no
            .mat(MatAtom.BOOL_EXPORTABLE, 0)  // no
            .writeable(true)
            .mat(MatAtom.VALIDATION, 128);

        // Send button (sibling)
        stream.startSibling(ObjectType.TRIGGER, "Send")
            .mat(MatAtom.BOOL_DEFAULT)
            .mat(MatAtom.COLOR_FACE, buttonColorFace)
            .mat(MatAtom.COLOR_TEXT, buttonColorText)
            .mat(MatAtom.COLOR_TOP_EDGE, buttonColorTopEdge)
            .mat(MatAtom.COLOR_BOTTOM_EDGE, buttonColorBottomEdge)
            .triggerStyle(TriggerStyle.PLACE)
            .fontId(FontId.ARIAL)
            .fontSize(10)
            .fontStyle(FontStyle.BOLD);

        // Send button action
        addSendButtonAction(stream);

        stream.endObject();  // End Send trigger

        // Close all nested objects
        stream.endObject();  // End input area org_group
        stream.endObject();  // End chat area org_group
        stream.endObject();  // End toolbar container org_group
        stream.endObject();  // End row container org_group
        stream.endObject();  // End outer org_group

        // Final setup
        stream.updateDisplay();
        stream.atom(P3Atom.INTERLEAVED_MODE, (byte) 0x01);

        // Criterion 130 action (update people count)
        addPeopleCountUpdateAction(stream);

        // Criterion 7 action (close handling)
        addCloseAction(stream);

        stream.uni(UniAtom.END_STREAM);

        return stream.compile();
    }

    private void addListRoomsAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action ->
            action.uni(UniAtom.START_STREAM)
                  .sendTokenArg("LP")
        );
    }

    private void addCenterStageAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action ->
            action.uni(UniAtom.START_STREAM)
                  .sm(SmAtom.SEND_K1, CENTER_STAGE_TARGET)
        );
    }

    private void addPcStudioAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action ->
            action.uni(UniAtom.START_STREAM)
                  .sm(SmAtom.SEND_K1, PC_STUDIO_TARGET)
        );
    }

    private void addPreferencesAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action ->
            action.uni(UniAtom.START_STREAM)
                  .man(ManAtom.PRESET_GID, PREFERENCES_FORM)
                  .if_(IfAtom.LAST_RETURN_TRUE_THEN, 1, 2)
                  .makeFocus()
                  .updateDisplay()
                  .uni(UniAtom.SYNC_SKIP, 1)
                  .waitOn()
                  .uni(UniAtom.INVOKE_NO_CONTEXT, PREFERENCES_FORM)
                  .uni(UniAtom.SYNC_SKIP, 2)
                  .endStream()
        );
    }

    private void addUserListSelectAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action -> {
            action.startStream()
                // Start extraction
                .de(DeAtom.START_EXTRACTION, 0)
                .de(DeAtom.SET_DATA_TYPE, DataType.GLOBAL_ID.code())
                .de(DeAtom.GET_DATA_VALUE, "current_object")
                .var(VarAtom.NUMBER_SET_FROM_ATOM, "A")
                .setContextRelative(TAG_USER_LIST)
                .de(DeAtom.SET_DATA_TYPE, DataType.RELATIVE_ID.code())
                .de(DeAtom.GET_DATA_VALUE)
                .var(VarAtom.NUMBER_SET_FROM_ATOM, "B")
                .de(DeAtom.END_EXTRACTION)
                .atom(BufAtom.CLEAR_BUFFER)
                .atom(BufAtom.CLOSE_BUFFER)
                .endContext()

                // Preset and invoke profile form
                .man(ManAtom.PRESET_GID, PROFILE_FORM)
                .if_(IfAtom.LAST_RETURN_TRUE_THEN, 1, 2)
                .makeFocus()
                .man(ManAtom.USE_DEFAULT_TITLE)
                .uni(UniAtom.USE_LAST_ATOM_STRING, "mat_title")
                .setContextRelative(1)
                .man(ManAtom.USE_DEFAULT_TITLE)
                .uni(UniAtom.USE_LAST_ATOM_STRING, "man_replace_data")
                .man(ManAtom.SET_DEFAULT_TITLE)
                .man(ManAtom.USE_DEFAULT_TITLE)
                .endContext()
                .uni(UniAtom.SYNC_SKIP, 1)
                .invokeLocal(PROFILE_FORM)
                .uni(UniAtom.SYNC_SKIP, 2)

                // Variable manipulation
                .var(VarAtom.NUMBER_SAVE, "A", 1)
                .var(VarAtom.NUMBER_SAVE, "B", 2)
                .var(VarAtom.NUMBER_GET, "A")
                .uni(UniAtom.USE_LAST_ATOM_VALUE, "man_set_context_globalid")
                .var(VarAtom.LOOKUP_BY_ID, "C", 65537)
                .var(VarAtom.NUMBER_GET, "B")
                .uni(UniAtom.USE_LAST_ATOM_VALUE, "man_set_context_relative")
                .var(VarAtom.NUMA_ZERO)
                .var(VarAtom.LOOKUP_BY_ID, "A", 65536)
                .endContext()
                .endContext()
                .var(VarAtom.NUMBER_SAVE, "A", 3)
                .var(VarAtom.NUMBER_SAVE, "C", 5)
                .setContextRelative(103)
                .var(VarAtom.NUMBER_GET, "A")
                .uni(UniAtom.USE_LAST_ATOM_VALUE, "mat_value")
                .endContext()
                .var(VarAtom.LOOKUP_BY_ID, "A", 2)
                .var(VarAtom.LOOKUP_BY_ID, "B", 5)
                .if_(IfAtom.NUMA_EQ_NUMB_THEN, 1, 2)
                .setContextRelative(103)
                .mat(MatAtom.BOOL_DISABLED, 1)  // yes
                .endContext()
                .uni(UniAtom.SYNC_SKIP, 1)
                .setContextRelative(103)
                .mat(MatAtom.BOOL_DISABLED, 0)  // no
                .endContext()
                .uni(UniAtom.SYNC_SKIP, 2)
                .man(ManAtom.UPDATE_WOFF_END_STREAM);
        });
    }

    private void addUserListSecondAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action ->
            action.uni(UniAtom.START_STREAM)
                  .waitOn()
                  .uni(UniAtom.INVOKE_NO_CONTEXT, PREFERENCES_FORM)
                  .waitOff()
                  .endStream()
        );
    }

    private void addChatAreaSelectAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action -> {
            action.startStream()
                .man(ManAtom.DO_MAGIC_RESPONSE_ID, 2097320)
                .de(DeAtom.START_EXTRACTION, 0)
                .de(DeAtom.SET_DATA_TYPE, DataType.GLOBAL_ID.code())
                .de(DeAtom.GET_DATA_VALUE, "current_object")
                .var(VarAtom.NUMBER_SET_FROM_ATOM, "A")
                .setContextRelative(TAG_USER_LIST)
                .de(DeAtom.SET_DATA_TYPE, DataType.RELATIVE_ID.code())
                .atom(BufAtom.CLEAR_BUFFER)
                .de(DeAtom.GET_DATA_VALUE)
                .var(VarAtom.NUMBER_SET_FROM_ATOM, "B")
                .de(DeAtom.END_EXTRACTION)
                .atom(BufAtom.CLOSE_BUFFER)
                .endContext()
                .invokeLocal(PROFILE_FORM)
                .waitOff()
                .endStream();
        });
    }

    private void addSendButtonAction(StreamBuilder stream) throws FdoException {
        stream.replaceSelectAction(action -> {
            action.startStream()
                .de(DeAtom.START_EXTRACTION)
                .de(DeAtom.VALIDATE, "display_msg | terminate")
                .atom(BufAtom.SET_TOKEN, "Aa")
                .setContextRelative(TAG_INPUT_FIELD)
                .de(DeAtom.GET_DATA)
                .man(ManAtom.CLEAR_OBJECT)
                .makeFocus()
                .updateDisplay()
                .de(DeAtom.END_EXTRACTION)
                .atom(BufAtom.CLOSE_BUFFER)
                .endContext()
                .endStream();
        });
    }

    private void addPeopleCountUpdateAction(StreamBuilder stream) throws FdoException {
        stream.act(ActAtom.SET_CRITERION, 130);
        stream.replaceAction(action -> {
            action.startStream()
                .setContextRelative(TAG_USER_LIST)
                .man(ManAtom.GET_CHILD_COUNT)
                .uni(UniAtom.CONVERT_LAST_ATOM_DATA)
                .uni(UniAtom.SAVE_RESULT)
                .man(ManAtom.CHANGE_CONTEXT_RELATIVE, TAG_PEOPLE_COUNT)
                .uni(UniAtom.GET_RESULT)
                .uni(UniAtom.USE_LAST_ATOM_STRING, "man_replace_data")
                .endContext()
                .updateDisplay()
                .endStream();
        });
    }

    private void addCloseAction(StreamBuilder stream) throws FdoException {
        stream.act(ActAtom.SET_CRITERION, 7);
        stream.replaceAction(action -> {
            action.startStream()
                .setContextGlobalId(CLOSE_CONTEXT)
                .act(ActAtom.DO_ACTION, "close")
                .endContext()
                .man(ManAtom.CLOSE, CLOSE_CONTEXT)
                .endStream();
        });
    }

    /**
     * Decompile this ChatRoom back to FDO text format.
     */
    public String decompile() throws FdoException {
        return FdoScript.decompile(compile());
    }

    // ========== Example main ==========

    public static void main(String[] args) throws FdoException {
        ChatRoom chatRoom = new ChatRoom()
            .buttonColorFace("C0C0C0")
            .buttonColorText("000000");

        byte[] binary = chatRoom.compile();
        System.out.println("Compiled to " + binary.length + " bytes");

        String text = chatRoom.decompile();
        System.out.println("\nDecompiled FDO:\n" + text);
    }
}
