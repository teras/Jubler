/*
 * TMPGencSubtitleEvent.java
 *
 * Created on 10-Jan-2009 by Hoang Duy Tran <hoang_tran>
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
package com.panayotis.jubler.subs.loader.processor.TMPGenc;

import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.records.TMPGenc.TMPGencSubtitleRecord;
import com.panayotis.jubler.time.Time;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencSubtitleEvent extends SubtitlePatternProcessor implements TMPGencPatternDef {

    /**
     * Pattern to recognize the TMPGenc's subtitle detail line
     * Typical example:
     * <pre>
     * 15,1,"00:02:29,001","00:02:32,014",0,"'The morning it all began,\nbegan like any other morning.'"
     * </pre>
     */
    private static final String pattern =
            digits +
            single_comma +
            digits +
            single_comma +
            TMPG_TIME +
            single_comma +
            TMPG_TIME +
            single_comma +
            digits +
            single_comma +
            printable;
    int index[] = new int[]{1, 2};

    public TMPGencSubtitleEvent() {
        super(pattern);
        setTargetObjectClassName(TMPGencSubtitleRecord.class.getName());
    //setMatchIndexList(index);
    }

    public void parsePattern(String[] matched_data, Object record) {
        //System.out.println(matched_data[0]);
        try {
            TMPGencSubtitleRecord r = (TMPGencSubtitleRecord) record;
            int id = Integer.parseInt(matched_data[1]);
            int stream_id = Integer.parseInt(matched_data[2]);
            r.setStreamID(stream_id);

            Time start, finish;
            start = new Time(matched_data[3], matched_data[4], matched_data[5], matched_data[6]);
            finish = new Time(matched_data[7], matched_data[8], matched_data[9], matched_data[10]);

            r.setStartTime(start);
            r.setFinishTime(finish);

            int layout_idx = Integer.parseInt(matched_data[11]);
            r.setLayoutIndex(layout_idx);

            String val = matched_data[12];
            //remove the leading double-quote (")
            if (val.startsWith(char_double_quote)) {
                val = val.substring(1);
            }

            //remove the trailing double-quote (")
            int len = val.length();
            if (val.endsWith(char_double_quote)) {
                val = val.substring(0, len - 1);
            }

            //check to see if there are double-quote
            //if there is, split them to a list. One
            //instance of the double-quote is remove, the
            //remaining instance is set to empty, thus, when
            //seeing this empty line, the double-quote is put-back
            //to retain original intention of the string.
            String[] list = val.split(char_double_quote);
            StringBuilder bld = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                String s = list[i];
                if (s == null || s.length() == 0) {
                    bld.append(char_double_quote);
                } else {
                    bld.append(s);
                }
            }//for(int i=0; i < list.length; i++)
            val = bld.toString();

            list = val.split(CHAR_TMPG_NEW_LINE_READ);
            bld = new StringBuilder();
            for (int i = 0; i < list.length; i++) {
                String s = list[i];
                bld.append(s);
                bld.append("\n");
            }//for(int i=0; i < list.length; i++)
            val = bld.toString().trim();
            r.setText(val);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//end if
}
