/*
 * SubRip.java
 *
 * Created on 26 Αύγουστος 2005, 11:08 πμ
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

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.loader.text.format.StyledFormat;
import com.panayotis.jubler.subs.loader.text.format.StyledTextSubFormat;
import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import java.util.Vector;


/**
 *
 * @author teras
 */
public class SubRip extends StyledTextSubFormat {
    
    private static final Pattern pat, stylepat;
    private static final Vector<StyledFormat> sdict;
    
    private int counter = 0;
    
    static {
        pat = Pattern.compile(
                "(?s)(\\d+)"+sp+nl+"(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)"+sp+"-->"+
                sp+"(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)"+sp+"(X1:\\d.*?)??"+nl+"(.*?)"+nl+nl
                );
        stylepat = Pattern.compile("<(.*?)>");
        
        sdict = new Vector<StyledFormat>();
        sdict.add(new StyledFormat(ITALIC, "i", true));
        sdict.add(new StyledFormat(ITALIC, "/i", false));
        sdict.add(new StyledFormat(BOLD, "b", true));
        sdict.add(new StyledFormat(BOLD, "/b", false));
        sdict.add(new StyledFormat(UNDERLINE, "u", true));
        sdict.add(new StyledFormat(UNDERLINE, "/u", false));
        sdict.add(new StyledFormat(STRIKETHROUGH, "s", true));
        sdict.add(new StyledFormat(STRIKETHROUGH, "/s", false));
    }
    
    protected Pattern getPattern() { return pat; }
    
    protected Pattern getStylePattern() { return stylepat; }
    protected String getTokenizer() { return "><"; } // Should not be useful
    protected String getEventIntro() { return "<"; }
    protected String getEventFinal() { return ">"; }
    protected String getEventMark() { return ""; }
    protected boolean isEventCompact() { return false; }
    
    
    protected Vector<StyledFormat> getStylesDictionary() { return sdict; }
    
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        SubEntry entry = new SubEntry(start, finish, m.group(11));
        entry.setStyle(subtitle_list.getStyleList().get(0));
        parseSubText(entry);
        return entry;
    }
    
    
    public String getExtension() {
        return "srt";
    }
    
    public String getName() {
        return "SubRip";
    }
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        str.append(Integer.toString(counter++));
        str.append("\n");
        str.append(sub.getStartTime().getSeconds());
        str.append(" --> ");
        str.append(sub.getFinishTime().getSeconds());
        str.append("\n");
        str.append(rebuildSubText(sub));
        str.append("\n\n");
    }
    
    protected void initSaver(Subtitles subs, MediaFile media, StringBuffer header) {
        counter = 1;
    }
    
    public boolean supportsFPS() { return false; }
}
