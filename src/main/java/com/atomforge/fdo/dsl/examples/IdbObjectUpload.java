package com.atomforge.fdo.dsl.examples;

import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.FdoScript;
import com.atomforge.fdo.dsl.atoms.IdbAtom;
import com.atomforge.fdo.dsl.atoms.ManAtom;
import com.atomforge.fdo.dsl.atoms.UniAtom;
import com.atomforge.fdo.model.FdoGid;

/**
 * Example: IDB (Internal Database) object upload using the FDO DSL.
 *
 * This demonstrates uploading binary data to the internal database,
 * showing how to use byte arrays with the DSL.
 *
 * Original FDO text template:
 * <pre>
 * uni_start_stream
 *     idb_delete_obj <{{ATOM_GID}}>
 *     uni_start_stream
 *         idb_start_obj <"{{IDB_OBJ_TYPE}}">
 *         idb_dod_progress_gauge <"Please wait while we add new data.">
 *         idb_atr_globalid <{{ATOM_GID}}>
 *         idb_atr_length <{{DATA_LENGTH}}>
 *         idb_atr_dod <01x>
 *         idb_atr_offset <0>
 *         idb_append_data <{{IDB_APPEND_DATA}}>
 *         idb_end_obj
 *     uni_end_stream
 * man_update_woff_end_stream
 * </pre>
 */
public final class IdbObjectUpload {

    private FdoGid objectGid;
    private String objectType = "data";
    private String progressMessage = "Please wait while we add new data.";
    private byte[] data;
    private int offset = 0;
    private byte dodFlag = 0x01;

    /**
     * Create a new IdbObjectUpload builder.
     */
    public IdbObjectUpload() {
    }

    /**
     * Set the GID for the object being uploaded.
     */
    public IdbObjectUpload gid(FdoGid gid) {
        this.objectGid = gid;
        return this;
    }

    /**
     * Set the GID from components.
     */
    public IdbObjectUpload gid(int type, int subtype, int id) {
        this.objectGid = FdoGid.of(type, subtype, id);
        return this;
    }

    /**
     * Set the object type string.
     */
    public IdbObjectUpload objectType(String type) {
        this.objectType = type;
        return this;
    }

    /**
     * Set the progress message shown during upload.
     */
    public IdbObjectUpload progressMessage(String message) {
        this.progressMessage = message;
        return this;
    }

    /**
     * Set the binary data to upload.
     */
    public IdbObjectUpload data(byte[] data) {
        this.data = data;
        return this;
    }

    /**
     * Set the data offset (default 0).
     */
    public IdbObjectUpload offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Set the DOD flag byte (default 0x01).
     */
    public IdbObjectUpload dodFlag(byte flag) {
        this.dodFlag = flag;
        return this;
    }

    /**
     * Compile this IDB upload command to FDO binary format.
     *
     * @return The compiled binary FDO stream
     * @throws FdoException if compilation fails
     * @throws IllegalStateException if required fields are not set
     */
    public byte[] compile() throws FdoException {
        if (objectGid == null) {
            throw new IllegalStateException("Object GID is required");
        }
        if (data == null) {
            throw new IllegalStateException("Data is required");
        }

        return FdoScript.stream()
            // Start outer stream
            .uni(UniAtom.START_STREAM)

            // Delete any existing object with this GID
            .idb(IdbAtom.DELETE_OBJ, objectGid)

            // Start inner stream for the new object
            .uni(UniAtom.START_STREAM)
                .idb(IdbAtom.START_OBJ, objectType)
                .idb(IdbAtom.DOD_PROGRESS_GAUGE, progressMessage)
                .idb(IdbAtom.ATR_GLOBALID, objectGid)
                .idb(IdbAtom.ATR_LENGTH, data.length)
                .idb(IdbAtom.ATR_DOD, new byte[] { dodFlag })
                .idb(IdbAtom.ATR_OFFSET, offset)
                .idb(IdbAtom.APPEND_DATA, data)
                .idb(IdbAtom.END_OBJ)
            .uni(UniAtom.END_STREAM)

            // Update display, turn off wait cursor, and end stream
            .man(ManAtom.UPDATE_WOFF_END_STREAM)
            .compile();
    }

    /**
     * Decompile this upload command back to FDO text format.
     */
    public String decompile() throws FdoException {
        return FdoScript.decompile(compile());
    }

    // ========== Static factory methods ==========

    /**
     * Create an IDB upload with the given GID and data.
     */
    public static IdbObjectUpload create(FdoGid gid, byte[] data) {
        return new IdbObjectUpload()
            .gid(gid)
            .data(data);
    }

    /**
     * Create an IDB upload with GID components and data.
     */
    public static IdbObjectUpload create(int type, int subtype, int id, byte[] data) {
        return new IdbObjectUpload()
            .gid(type, subtype, id)
            .data(data);
    }

    // ========== Example main for testing ==========

    public static void main(String[] args) throws FdoException {
        // Example: Upload some binary data to the IDB
        byte[] exampleData = "Hello, this is test data for IDB upload.".getBytes();

        IdbObjectUpload upload = IdbObjectUpload.create(
            FdoGid.of(1, 100, 12345),
            exampleData
        )
        .objectType("text/plain")
        .progressMessage("Please wait while we save your data.");

        // Compile to binary
        byte[] binary = upload.compile();
        System.out.println("Compiled to " + binary.length + " bytes");

        // Decompile back to text for verification
        String text = upload.decompile();
        System.out.println("\nDecompiled FDO:\n" + text);

        // Example with raw binary data
        byte[] binaryPayload = new byte[] { 0x00, 0x01, 0x02, 0x03, (byte)0xFF, (byte)0xFE };
        IdbObjectUpload binaryUpload = IdbObjectUpload.create(
            2, 50, 99999,
            binaryPayload
        );

        System.out.println("\nBinary payload upload (" + binaryPayload.length + " bytes):");
        System.out.println(binaryUpload.decompile());
    }
}
