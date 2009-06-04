/*
 * ScanTitle.java
 *
 * Created on 29 July 2006, Arno W. Peters
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

package com.panayotis.jubler.subs.loader.binary;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author teras
 */
public class ScanTitle extends AbstractBinarySubFormat {
    
    /** Creates a new instance of ScanTitle */
    public ScanTitle() {
    }
    
    public String getExtension() {
        return "890";
    }
    
    public String getName() {
        return "ScanTitle";
    }
    
    public boolean isSubType(String input, File f){
        return true;
    }
    public void parseBinary(float FPS, BufferedReader in) {
        // Add code to load subtitles from input buffer
        // Example:
        int subframestart;
        int subframeend;
        String subtext;
        
        //while (have more entries  , in.read())
            subframestart = 10; //
            subframeend = 20;
            subtext = "test";
            
            subtitle_list.add(new SubEntry(subframestart/FPS, subframeend/FPS, subtext));
        //}
    }

    public boolean supportsFPS() { return true; }

    public boolean produce(Subtitles subs, File out, MediaFile media) throws IOException {
        return true;
    }
    
    
}
