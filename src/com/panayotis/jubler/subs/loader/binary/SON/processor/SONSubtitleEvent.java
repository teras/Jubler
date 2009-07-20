/*
 * SONSubtitleEvent.java
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
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
import com.panayotis.jubler.time.Time;

/**
 * Process data entry in the following format:
 * <pre>
 * 0001		00:00:11:01	00:00:15:08	Edwardians In Colour _st00001p1.bmp
 * </pre>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SONSubtitleEvent extends SubtitlePatternProcessor implements CommonDef {

    private static String pattern = digits + sp + son_time + sp + son_time + sp + printable;
    int index[] = new int[]{1, 3, 4, 5, 6, 8, 9, 10, 11, 13};
    private SonSubEntry sonSubEntry = null;

    public SONSubtitleEvent() {
        super(pattern);
        setMatchIndexList(index);
    }

    public void parsePattern(String[] matched_data, Object record) {        
        if (record instanceof SonSubEntry) {
            setSonSubEntry((SonSubEntry) record);
            sonSubEntry.event_id = DVDMaestro.parseShort(matched_data[0]);

            Time start, finish;
            
            start = new Time(0);
            start.setTimeLiteral(matched_data[1], matched_data[2], matched_data[3], matched_data[4]);
            
            finish = new Time(0);
            finish.setTimeLiteral(matched_data[5], matched_data[6], matched_data[7], matched_data[8]);

            sonSubEntry.setStartTime(start);
            sonSubEntry.setFinishTime(finish);
            sonSubEntry.image_filename = matched_data[9];
        }//end if (record instanceof SonSubEntry)

    }//end if

    public SonSubEntry getSonSubEntry() {
        return sonSubEntry;
    }

    public void setSonSubEntry(SonSubEntry sonSubEntry) {
        this.sonSubEntry = sonSubEntry;
    }
}
