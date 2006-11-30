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

package com.panayotis.jubler.subs.format;

import com.panayotis.jubler.subs.format.types.AdvancedSubStation;
import com.panayotis.jubler.subs.format.types.MicroDVD;
import com.panayotis.jubler.subs.format.types.PlainText;
import com.panayotis.jubler.subs.format.types.ScanTitle;
import com.panayotis.jubler.subs.format.types.SubRip;
import com.panayotis.jubler.subs.format.types.SubStationAlpha;
import com.panayotis.jubler.subs.format.types.SubViewer;
import com.panayotis.jubler.subs.format.types.SubViewer2;

/**
 *
 * @author teras
 */
public class AvailSubFormats {
    public static final SubFormat []Formats;
    int current;
    
    static {
        Formats = new SubFormat [8];
        Formats[0] = new AdvancedSubStation();
        Formats[1] = new SubStationAlpha();
        Formats[2] = new SubRip();
        Formats[3] = new SubViewer2();
        Formats[4] = new SubViewer();
        Formats[5] = new MicroDVD();
        Formats[6] = new ScanTitle();
        Formats[7] = new PlainText();
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
