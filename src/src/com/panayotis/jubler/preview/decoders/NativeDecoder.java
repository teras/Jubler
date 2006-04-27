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

package com.panayotis.jubler.preview.decoders;

/**
 *
 * @author teras
 */
public abstract class NativeDecoder extends AbstractDecoder {
    
    public AudioCache getAudioCache(double from, double to) {
        if (!isDecoderValid()) return null;
        if (cfname==null) return null;
        
        float[] data = grabCache(cfname, from, to);
        if (data==null) return null;
        return new AudioCache(data);
    }
    
    public void forgetAudioCache() {
        if (!isDecoderValid()) return;
        if (cfname==null) return;
        
        forgetCache(cfname);
        cfname=null;
    }
    
    private native float[] grabCache(String cfile, double from, double to);
    private native void forgetCache(String cfile);
}
