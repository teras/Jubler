/*
 * SONDisplayArea.java
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
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
/**
 * Process data entry in the following format:
 * <pre>
 * Display_Area	(000 446 720 518)
 * </pre>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SONDisplayArea extends SubtitlePatternProcessor implements CommonDef {

    private static String pattern = "Display_Area" + sp + "\\(" + sp_digits + sp + digits + sp + digits + sp + digits + sp_maybe + "\\)";
    int index[] = new int[]{3, 5, 7, 9};
    private SonHeader sonHeader = null;
    private SonSubEntry sonSubEntry = null;

    public SONDisplayArea() {
        super(pattern);
        setMatchIndexList(index);
    }

    public void parsePattern(String[] matched_data, Object record) {
        short[] array = SonSubEntry.makeAttributeEntry(matched_data);
        if (record instanceof SonHeader) {
            setSonHeader((SonHeader) record);
            sonHeader.getCreateSonAttribute().display_area = array;
        }//end if (record instanceof SonHeader)

        if (record instanceof SonSubEntry) {
            setSonSubEntry((SonSubEntry) record);
            sonSubEntry.getCreateSonAttribute().display_area = array;
        }//end if (record instanceof SonSubEntry)
    }//end if

    public SonHeader getSonHeader() {
        return sonHeader;
    }

    public void setSonHeader(SonHeader sonHeader) {
        this.sonHeader = sonHeader;
    }

    public SonSubEntry getSonSubEntry() {
        return sonSubEntry;
    }

    public void setSonSubEntry(SonSubEntry sonSubEntry) {
        this.sonSubEntry = sonSubEntry;
    }
}
