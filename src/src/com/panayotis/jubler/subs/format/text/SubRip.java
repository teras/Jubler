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

package com.panayotis.jubler.subs.format.text;

import com.panayotis.jubler.subs.format.AbstractTextSubFormat;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Subtitles;


/**
 *
 * @author teras
 */
public class SubRip extends AbstractTextSubFormat {
    
    private static final Pattern pat;
    
    private int counter = 0;
    
    static {
        pat = Pattern.compile(
                "(?s)(\\d+)\\s*"+nl+"(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)\\s+-->"+
                "\\s+(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d\\d)"+nl+"(.*?)"+nl+nl
                );
    }
    
    protected Pattern getPattern () {
        return pat;
    }
    
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        return new SubEntry (start, finish, m.group(10));
    }
    
    
    public String getExtension() {
        return "srt";
    }
    
    public String getName() {
        return "SubRip";
    }
      
    protected String makeSubEntry(SubEntry sub){
        StringBuffer res;
        res = new StringBuffer();
        
        res.append(Integer.toString(counter++));
        res.append("\n");
        res.append(sub.getStartTime().toString());
        res.append(" --> ");
        res.append(sub.getFinishTime().toString());
        res.append("\n");
        res.append(sub.getText());
        res.append("\n\n");
        
        return res.toString();
    }
    
    
    protected String initLoader(String input, Subtitles subs) {
        return super.initLoader(input, subs);
    }

    protected String makeHeader(Subtitles subs) {
        counter = 1;
        return super.makeHeader(subs);
    }

    public boolean supportsFPS() {
        return false;
    }
}
