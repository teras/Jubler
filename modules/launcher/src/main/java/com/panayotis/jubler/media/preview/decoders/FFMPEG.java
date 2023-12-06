/*
 * FFMPEG.java
 *
 * Created on 25 Σεπτέμβριος 2005, 7:12 μμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.media.preview.decoders;

/**
 * @author teras
 */
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
