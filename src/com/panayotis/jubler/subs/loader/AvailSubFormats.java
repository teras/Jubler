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

import com.panayotis.jubler.subs.loader.binary.SON.DVDMaestro;
import com.panayotis.jubler.subs.loader.binary.SWT.DVDMaestroExtendedSWT;
import com.panayotis.jubler.subs.loader.binary.SUP.SUPCompressedImage;
import com.panayotis.jubler.subs.loader.text.AdvancedSubStation;
import com.panayotis.jubler.subs.loader.text.MPL2;
import com.panayotis.jubler.subs.loader.text.MicroDVD;
import com.panayotis.jubler.subs.loader.text.PlainOCRTextWithDoubleNewLine;
import com.panayotis.jubler.subs.loader.text.PlainOCRTextWithPageBreak;
import com.panayotis.jubler.subs.loader.text.PlainText;
import com.panayotis.jubler.subs.loader.text.Quicktime;
import com.panayotis.jubler.subs.loader.text.Spruce;
import com.panayotis.jubler.subs.loader.text.SubRip;
import com.panayotis.jubler.subs.loader.text.SubStationAlpha;
import com.panayotis.jubler.subs.loader.text.SubViewer;
import com.panayotis.jubler.subs.loader.text.SubViewer2;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.TMPGenc;
import com.panayotis.jubler.subs.loader.text.W3CTimedText;

/**
 *
 * @author teras and Hoang Duy Tran
 */
public class AvailSubFormats {

    public static final SubFormat[] Formats = {
        new SUPCompressedImage(),
        new TMPGenc(),
        new DVDMaestroExtendedSWT(), //added by HDT
        new DVDMaestro(), //added by HDT
        new AdvancedSubStation(),
        new SubStationAlpha(),
        new SubRip(),
        new SubViewer2(),
        new SubViewer(),
        new MicroDVD(),
        new MPL2(),
        new Spruce(),
        new Quicktime(),
        new W3CTimedText(),
        new PlainOCRTextWithPageBreak(), //added by HDT        
        new PlainOCRTextWithDoubleNewLine(), //added by HDT
        new PlainText()
    //new ScanTitle()
    };
    int current;

    /** Creates a new instance of SubFormats */
    public AvailSubFormats() {
        current = 0;
    }

    public boolean hasMoreElements() {
        if (current < Formats.length)
            return true;
        return false;
    }

    public SubFormat nextElement() {
        SubFormat handler = Formats[current];
        current++;
        return handler;
    }

    public static SubFormat findFromDescription(String name) {
        for (int i = 0; i < Formats.length; i++)
            if (Formats[i].getDescription().equals(name))
                return Formats[i];
        return null;
    }

    public static SubFormat findFromName(String ext) {
        for (int i = 0; i < Formats.length; i++)
            if (Formats[i].getName().equals(ext))
                return Formats[i];
        return null;
    }
}
