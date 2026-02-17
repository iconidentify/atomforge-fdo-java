package com.atomforge.fdo;

/**
 * Callback interface for receiving FDO binary output in frame-sized chunks.
 *
 * <p>This interface enables streaming compilation where output is produced
 * incrementally in chunks suitable for P3 framing or other size-limited
 * transport protocols.</p>
 *
 * <p>Key guarantees:
 * <ul>
 *   <li>Atoms are never split across frame boundaries</li>
 *   <li>Large atoms are handled using the UNI continuation protocol</li>
 *   <li>Frame size is configurable per compilation</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * FdoCompiler compiler = FdoCompiler.create();
 * compiler.compileToFrames(fdoSource, 119, (frameData, index, isLast) -> {
 *     // Wrap in P3 frame and send
 *     connection.send(wrapInP3Frame(frameData));
 * });
 * }</pre>
 *
 * @see FdoCompiler#compileToFrames(String, int, FrameConsumer)
 */
@FunctionalInterface
public interface FrameConsumer {

    /**
     * Called when a complete frame of atom data is ready.
     *
     * <p>Each frame contains one or more complete atoms (never partial atoms).
     * The frame size will not exceed the maximum specified in the compile call,
     * but may be smaller if atoms don't fill the frame completely.</p>
     *
     * @param frameData   The encoded atom bytes for this frame
     * @param frameIndex  Zero-based sequence number (0 for first frame)
     * @param isLastFrame True if this is the final frame of the compilation
     */
    void onFrame(byte[] frameData, int frameIndex, boolean isLastFrame);
}
