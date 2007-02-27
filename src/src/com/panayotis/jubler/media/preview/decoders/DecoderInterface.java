/*
 * DecoderInterface.java
 *
 * Created on October 3, 2005, 4:21 PM
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

import java.awt.Dimension;
import java.awt.Image;

/**
 *
 * @author teras
 */
public interface DecoderInterface {
    
    public abstract boolean isDecoderValid();

    public abstract boolean initAudioCache(String afile, String cfile, DecoderListener fback);
    public abstract void setInterruptStatus(boolean interrupt);
    public abstract boolean getInterruptStatus();
    public abstract void closeAudioCache(String cache);

    public abstract AudioPreview getAudioPreview(String cache, double from, double to);

    public abstract Image getFrame(String video, double time, boolean small);
    public abstract float getFPS(String vfile);
    public abstract Dimension getDimension(String vfile);
    
    public abstract void playAudioClip(String audio, double from, double to);
    
}
