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
package com.panayotis.jubler.subs.loader.binary.TMPGenc.processor;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.TMPGencPatternDef;
import com.panayotis.jubler.subs.loader.binary.TMPGenc.record.TMPGencSubtitleRecord;
import com.panayotis.jubler.time.Time;
import java.util.logging.Level;

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
            digits + //id
            single_comma +
            digits + //visibility
            single_comma +
            TMPG_TIME + //start-time
            single_comma +
            TMPG_TIME + //finish-time
            single_comma +
            digits + //layout-index
            single_comma;

    int index[] = new int[]{1, 3, 5, 6, 7, 8, 10, 11, 12, 13, 15};
    
    public TMPGencSubtitleEvent() {
        super(pattern);
        setMatchIndexList(index);
        setTargetObjectClassName(TMPGencSubtitleRecord.class.getName());
        
    }

    public void parsePattern(String[] matched_data, Object record) {
        try {
            TMPGencSubtitleRecord r = (TMPGencSubtitleRecord) record;
            int id = Integer.parseInt(matched_data[0]);
            r.setId(id);

            int stream_id = Integer.parseInt(matched_data[1]);
            r.setEnabled(stream_id);

            Time start, finish;
            start = new Time(matched_data[2], matched_data[3], matched_data[4], matched_data[5]);
            finish = new Time(matched_data[6], matched_data[7], matched_data[8], matched_data[9]);

            r.setStartTime(start);
            r.setFinishTime(finish);

            int layout_idx = Integer.parseInt(matched_data[10]);
            r.setLayoutIndex(layout_idx);

            String txt = getSubTextManually();
            
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
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//end if

    /**
     * This is to cope with last line of the subtitle file where sub-text exists
     * but without the surrounding double-quotes, leading to error in the parsing
     * of the pattern.
     * 1203,1,"01:37:50,000","01:37:52,002",0,#Aaaaah!
     * @return subttile-text filtered or empty string if no text exists.
     */
    private String getSubTextManually() {
        String result_txt = null;
        String txt = getTextLine();
        String split_pattern =
                char_double_quote +
                char_comma +
                digits +
                char_comma;
        String[] list = txt.split(split_pattern);
        boolean valid = (list.length == 2);
        if (valid) {
            result_txt = list[1];
        } else {
            result_txt = "";
        }//end if (valid)
        return result_txt;
    }
}
