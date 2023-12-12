/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.os.DEBUG;

public class NativeDecoder {
    private final NativeDecoderCallback callback;

    public NativeDecoder(NativeDecoderCallback callback) {
        this.callback = callback;
    }

    public native boolean makeCache(String afile, String cfile, String aname);

    public native float[] grabCache(String cfile, double from, double to);

    public native void forgetCache(String cfile);

    /*
     * Callbacks
     */

    /* This is also a callback function to use the standard DEBUG object in C */
    public void debug(String debug) {
        DEBUG.debug(debug);
    }

    /* This is a method to be called by native routines in order to check
     * the status of the produced cache */
    private void updateViewport(float position) {
        callback.updateViewport(position);
    }

    /* This method is used again as a callback, to see if the user clicked on the cancel button */
    public boolean getInterruptStatus() {
        return callback.getInterruptStatus();
    }
}
