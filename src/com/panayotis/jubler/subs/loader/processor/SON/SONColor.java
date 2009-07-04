/*
 * SONColor.java
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
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
/**
 * This class is used to process the colour component of DVDMaestro format file.
 * It process the colour data entry in the following format:
 * <pre>
 * Color	(0 1 6 7)
 * </pre>
 * A short array is created for the values parsed, and is stored in either
 * the {@link SonSubEntry#colour} or {@link SonHeader#colour}, depending on
 * the object currently active, indicating where the component was found.
 * @see SonSubEntry
 * @see SonHeader
 * @see com.panayotis.jubler.subs.loader.binary.DVDMaestro
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SONColor extends SubtitlePatternProcessor implements CommonDef {
    
    /**
     * The pattern for data  <pre>Color	(0 1 6 7)</pre>
     */
    private static String pattern = "Color" + sp + "\\(" + sp_digits + sp + digits + sp + digits + sp + digits + sp_maybe + "\\)";   
    /**
     * The index where matched groups of data will be found.
     */
    int index[] = new int[]{3, 5, 7, 9};
    /**
     * The reference to header record.
     */
    private SonHeader sonHeader = null;
    /**
     * The reference to the subtitle-entry record.
     */
    private SonSubEntry sonSubEntry = null;

    /**
     * Default construction, set the pattern and the list of matching 
     * indices where data groups should be found.
     */
    public SONColor() {
        super(pattern);
        setMatchIndexList(index);
    }

    /**
     * For the matched group of data, a short array is created and all
     * digits representing selection of colours 
     * @param matched_data
     * @param record
     */
    public void parsePattern(String[] matched_data, Object record) {
        short[] array = SonSubEntry.makeAttributeEntry(matched_data);
        if (record instanceof SonHeader) {
            sonHeader = (SonHeader) record;
            sonHeader.getCreteSonAttribute().colour = array;
        }//end if (record instanceof SonHeader)

        if (record instanceof SonSubEntry) {
            setSonSubEntry((SonSubEntry) record);
            sonSubEntry.getCreteSonAttribute().colour = array;
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
