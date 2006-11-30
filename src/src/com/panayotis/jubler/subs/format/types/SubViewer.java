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

package com.panayotis.jubler.subs.format.types;

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
public class SubViewer extends AbstractTextSubFormat {
    
    private static final Pattern pat, testpat;
    private static final Pattern title, author, source, comments;
    
    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile(
                "(?s)(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"+
                space+nl+"(.*?)"+nl+nl
                );
        
        testpat = Pattern.compile("(?i)(?s)\\[INFORMATION\\].*?"+
                "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"+
                space+nl+"(.*?)"+nl+nl
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
     
    
    protected String makeSubEntry(SubEntry sub){
        StringBuffer res;
        String t;
        
        res = new StringBuffer();
        
        t = sub.getStartTime().toString().replace(',','.');
        t = t.substring(0, t.length()-1);
        res.append(t);
        
        res.append(',');
        
        t = sub.getFinishTime().toString().replace(',','.');
        t = t.substring(0, t.length()-1);
        res.append(t);
        
        res.append("\n");
        res.append(subreplace(sub.getText()));
        res.append("\n\n");
        
        return res.toString();
    }
    
    protected String subreplace(String sub) {
        return sub;
    }
    
    protected String makeHeader(Subtitles subs) {
        StringBuffer header = new StringBuffer();
        
        header.append("[INFORMATION]\n[TITLE]");
        header.append(subs.getAttrib("title"));
        header.append("\n[AUTHOR]");
        header.append(subs.getAttrib("author"));
        header.append("\n[SOURCE]");
        header.append(subs.getAttrib("source"));
        header.append("\n[FILEPATH]\n[DELAY]0\n[COMMENT]");
        header.append(subs.getAttrib("comments").replace('\n', ' '));
        header.append("\n[END INFORMATION]\n[SUBTITLE]\n[COLF]&HFFFFFF,[STYLE]bd,[SIZE]18,[FONT]Arial\n");
        return header.toString();
    }
    
    protected String initLoader(String input, Subtitles subs) {
        input = super.initLoader(input, subs);
        Matcher m;
        
        m = title.matcher(input);
        if (m.find()) subs.setAttrib("title", m.group(1).trim());
        
        m = author.matcher(input);
        if (m.find()) subs.setAttrib("author", m.group(1).trim());
        
        m = source.matcher(input);
        if (m.find()) subs.setAttrib("source", m.group(1).trim());
        
        m = comments.matcher(input);
        if (m.find()) subs.setAttrib("comments", m.group(1).trim());
        
        return input;
    }
}
