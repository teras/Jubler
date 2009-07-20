/*
 * SONPixelArea.java
 *
 * Created on 11-Dec-2008 by Hoang Duy Tran <hoang_tran>
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.subs.loader.binary.SON.processor;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.loader.binary.SON.DVDMaestro;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;

/**
 * Process data entry in the following format:
 * <pre>
 * Pixel_Area	(0 575)
 * </pre>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SONPixelArea extends SubtitlePatternProcessor implements CommonDef {

    private static String pattern = "Pixel_Area" + sp + "\\(" + sp_digits + sp + digits + sp_maybe + "\\)";
    int index[] = new int[]{3, 5};
    private SonHeader sonHeader = null;

    public SONPixelArea() {
        super(pattern);
        setMatchIndexList(index);
    }

    public void parsePattern(String[] matched_data, Object record) {
        if (record instanceof SonHeader) {
            sonHeader = (SonHeader) record;
            sonHeader.pixel_area = new short[2];
            sonHeader.pixel_area[0] = DVDMaestro.parseShort(matched_data[0]);
            sonHeader.pixel_area[1] = DVDMaestro.parseShort(matched_data[1]);
        }//end if

    }//end if

    public SonHeader getSonHeader() {
        return sonHeader;
    }

    public void setSonHeader(SonHeader sonHeader) {
        this.sonHeader = sonHeader;
    }
}
