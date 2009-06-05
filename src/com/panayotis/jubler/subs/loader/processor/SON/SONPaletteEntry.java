/*
 * SONPaletteEntry.java
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
package com.panayotis.jubler.subs.loader.processor.SON;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.loader.binary.DVDMaestro;
import com.panayotis.jubler.subs.records.SON.SonHeader;

/**
 * Process data entry in the following format:
 * <pre>
 * # 00 : RGB(255,255, 0)
 * </pre>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SONPaletteEntry extends SubtitlePatternProcessor implements CommonDef {

    private static String rgb = "RGB\\(" + sp_digits + "," + sp_digits + "," + sp_digits + "\\)";
    private static String pattern = DVDMaestro.p_son_comment + sp + digits + sp + ":" + sp + rgb;
    int index[] = new int[]{2, 6, 8, 10};
    private SonHeader sonHeader = null;

    public SONPaletteEntry() {
        super(pattern);
        setMatchIndexList(index);
    }

    public void parsePattern(String[] matched_data, Object record) {
        if (record instanceof SonHeader) {
            sonHeader = (SonHeader) record;
            sonHeader.addPaletteEntry(
                    matched_data[0],
                    matched_data[1],
                    matched_data[2],
                    matched_data[3]);
            //System.out.println(sonHeader.toString());
        }//end if

    }//end if

    public SonHeader getSonHeader() {
        return sonHeader;
    }

    public void setSonHeader(SonHeader sonHeader) {
        this.sonHeader = sonHeader;
    }
}
