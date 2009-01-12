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
 * Pattern to recognize the TMPGenc's subtitle detail line
 * Typical example:
 * <blockquote><pre>
 * 15,1,"00:02:29,001","00:02:32,014",0,"""If lost, please return\nto Charles Christopher Schine."""
 * </pre></blockquote>
 * <ol>
 * <li> Subtitle event's ID, starting from 1
 * <li> Entry's visibility flag: 1=visible, 0=invisible.
 * When 0 is set, the subtitle event is not visisible upon playback of DVD.
 * <li> Start-time: The format is similar to SRT
 * <li> Finish-time: The format is similar to SRT
 * <li> Layout index: Index to the layout entry in the [LayoutData] section.
 * <li> Subtitle-text: Surrounded by double-quotes, and two double-quotes refers to one instance
 * of the double-quote in the text. New-line is replaced by the group "\\n" and thus
 * the whole subtitle text resides in a single-line, so parsing and replacing must use '\\\\n'.
 * </ol>
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencSubtitleEvent extends SubtitlePatternProcessor implements TMPGencPatternDef {

    private static final String pattern =
            digits +        //id
            single_comma +
            digits +        //visibility
            single_comma +
            TMPG_TIME +     //start-time
            single_comma +
            TMPG_TIME +     //finish-time
            single_comma +
            digits +        //layout-index
            single_comma +
            printable;      //subtitle-text

    public TMPGencSubtitleEvent() {
        super(pattern);
        setTargetObjectClassName(TMPGencSubtitleRecord.class.getName());
    }

    public void parsePattern(String[] matched_data, Object record) {
        try {
            TMPGencSubtitleRecord r = (TMPGencSubtitleRecord) record;
            int id = Integer.parseInt(matched_data[1]);
            r.setId(id);

            int stream_id = Integer.parseInt(matched_data[2]);
            r.setEnabled(stream_id);

            Time start, finish;
            start = new Time(matched_data[3], matched_data[4], matched_data[5], matched_data[6]);
            finish = new Time(matched_data[7], matched_data[8], matched_data[9], matched_data[10]);

            r.setStartTime(start);
            r.setFinishTime(finish);

            int layout_idx = Integer.parseInt(matched_data[11]);
            r.setLayoutIndex(layout_idx);

            String txt = matched_data[12];
            //remove the leading double-quote (")
            if (txt.startsWith(char_double_quote)) {
                txt = txt.substring(1);
            }

            //remove the trailing double-quote (")
            int len = txt.length();
            if (txt.endsWith(char_double_quote)) {
                txt = txt.substring(0, len - 1);
            }

            //correcting single-line of text with patched double-quotes and new-lines
            String r_txt = txt.replaceAll(char_two_double_quotes, char_double_quote);
            txt = r_txt.replaceAll(pat_nl, UNIX_NL);

            r.setText(txt);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//end if
}
