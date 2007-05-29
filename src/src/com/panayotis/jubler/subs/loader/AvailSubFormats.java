/*
 * SubFormats.java
 *
 * Created on 22 Ιούνιος 2005, 3:40 πμ
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

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.subs.loader.binary.DVDMaestro;
import com.panayotis.jubler.subs.loader.text.AdvancedSubStation;
import com.panayotis.jubler.subs.loader.text.MPL2;
import com.panayotis.jubler.subs.loader.text.MicroDVD;
import com.panayotis.jubler.subs.loader.text.PlainText;
import com.panayotis.jubler.subs.loader.text.Spruce;
import com.panayotis.jubler.subs.loader.text.SubRip;
import com.panayotis.jubler.subs.loader.text.SubStationAlpha;
import com.panayotis.jubler.subs.loader.text.SubViewer;
import com.panayotis.jubler.subs.loader.text.SubViewer2;

/**
 *
 * @author teras
 */
public class AvailSubFormats {
    public static final SubFormat []Formats;
    int current;
    
    static {
        Formats = new SubFormat [10];
        Formats[0] = new AdvancedSubStation();
        Formats[1] = new SubStationAlpha();
        Formats[2] = new SubRip();
        Formats[3] = new SubViewer2();
        Formats[4] = new SubViewer();
        Formats[5] = new MicroDVD();
        Formats[6] = new MPL2();
        Formats[7] = new Spruce();
        Formats[8] = new PlainText();
        Formats[9] = new DVDMaestro();
     //   Formats[7] = new ScanTitle();
    }
    
    /** Creates a new instance of SubFormats */
    public AvailSubFormats() {
        current = 0;
    }
    
    public boolean hasMoreElements() {
        if ( current < Formats.length ) return true;
        return false;
    }
    
    public SubFormat nextElement() {
        return Formats[current++];
    }
    
    public static SubFormat findFromDescription(String name) {
        for ( int i = 0 ; i < Formats.length ; i++ ) {
            if ( Formats[i].getDescription().equals(name)) {
                return Formats[i];
            }
        }
        return null;
    }
    
    public static SubFormat findFromName(String ext) {
        for ( int i = 0 ; i < Formats.length ; i++ ) {
            if ( Formats[i].getName().equals(ext)) {
                return Formats[i];
            }
        }
        return null;
    }
}
