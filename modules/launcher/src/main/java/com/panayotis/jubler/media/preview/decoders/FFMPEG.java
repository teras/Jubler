/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

public final class FFMPEG extends NativeDecoder {

    public FFMPEG(NativeDecoderCallback callback) {
        super(callback);
    }

    /* Get the image for this timestamp */
    public native byte[] grabFrame(String video, long time, float resize);

    /* Create a wav file from the specified time stamps */
    public native boolean createClip(String audio, String wav, long from, long to);

    /* Get the dimensions of a video file */
    public native float[] grabInformation(String vfile);
}
