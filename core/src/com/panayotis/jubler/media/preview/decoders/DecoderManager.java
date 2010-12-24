/*
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

import com.panayotis.jubler.plugins.PluginManager;

/**
 *
 * @author teras
 */
public class DecoderManager {

    private final static DecoderManager mgr = new DecoderManager();
    private AudioDecoder adec;
    private VideoDecoder vdec;

    public static VideoDecoder getVideoDecoder() {
        return mgr.vdec;
    }

    public static AudioDecoder getAudioDecoder() {
        return mgr.adec;
    }

    public void registerVideoDecoder(VideoDecoder vdec) {
        this.vdec = vdec;
    }

    public void registerAudioDecoder(AudioDecoder adec) {
        this.adec = adec;
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private DecoderManager() {
        PluginManager.manager.callPostInitListeners(this);
    }
}
