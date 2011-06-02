/*
 * SubViewer.java
 *
 * Created on 22 Ιούνιος 2005, 3:08 πμ
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

import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.Subtitles;


/**
 *
 * @author teras
 */
public class SubViewer extends AbstractTextSubFormat {
    
    private static final Pattern pat, testpat;
    private static final Pattern title, author, source, comments;
    
    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile(
                "(?s)(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"+
                sp+nl+"(.*?)"+nl+nl
                );
        
        testpat = Pattern.compile("(?i)(?s)\\[INFORMATION\\].*?"+
                "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"+
                sp+nl+"(.*?)"+nl+nl
                );
        
        title = Pattern.compile("(?i)\\[TITLE\\](.*?)"+nl);
        author = Pattern.compile("(?i)\\[AUTHOR\\](.*?)"+nl);
        source = Pattern.compile("(?i)\\[SOURCE\\](.*?)"+nl);
        comments = Pattern.compile("(?i)\\[COMMENT\\](.*?)"+nl);
    }
    
    protected Pattern getPattern() {
        return pat;
    }
    
    protected Pattern getTestPattern() {
        return testpat;
    }
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        Time finish = new Time(m.group(5), m.group(6), m.group(7), m.group(8));
        return new SubEntry ( start, finish, m.group(9).replaceAll("\\[br\\]", "\n"));
    }
    
    
    public String getExtension() {
        return "sub";
    }
    
    public String getName() {
        return "SubViewer";
    }
     
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        String t;

        t = sub.getStartTime().getSeconds().replace(',','.');
        t = t.substring(0, t.length()-1);
        str.append(t);
        
        str.append(',');
        
        t = sub.getFinishTime().getSeconds().replace(',','.');
        t = t.substring(0, t.length()-1);
        str.append(t);
        
        str.append("\n");
        str.append(subreplace(sub.getText()));
        str.append("\n\n");
    }
    
    protected String subreplace(String sub) {
        return sub;
    }
    
    protected void initSaver(Subtitles subs, MediaFile media, StringBuffer header) {
        SubAttribs attr = subs.getAttribs();
        header.append("[INFORMATION]\n[TITLE]");
        header.append(attr.getTitle());
        header.append("\n[AUTHOR]");
        header.append(attr.getAuthor());
        header.append("\n[SOURCE]");
        header.append(attr.getSource());
        header.append("\n[FILEPATH]\n[DELAY]0\n[COMMENT]");
        header.append(attr.getComments().replace('\n', '|'));
        header.append("\n[END INFORMATION]\n[SUBTITLE]\n[COLF]&HFFFFFF,[STYLE]bd,[SIZE]18,[FONT]Times New Roman\n");
    }
    
    protected String initLoader(String input) {
        input = super.initLoader(input);
        updateAttributes(input, title, author, source, comments);
        return input;
    }

    public boolean supportsFPS() { return false; }
}
