/*
 * NativeDecoder.java
 *
 * Created on 23 Οκτώβριος 2005, 8:09 μμ
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

import com.panayotis.jubler.os.DEBUG;

/**
 * @author teras
 */
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
