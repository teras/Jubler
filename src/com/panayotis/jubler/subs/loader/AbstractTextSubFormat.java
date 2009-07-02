/*
 * AbstractTextSubFormat.java
 *
 * Created on 22 Ιούνιος 2005, 3:17 πμ
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

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * This class provides a basic textual processing foundation class for
 * classes processing text based subtitle files. Each implemented instance
 * should provide a {@link #getTestPattern test-pattern}
 * for program to recognise its format signature, and a 
 * {@link #getPattern working-pattern} for parsing records. By default, 
 * the test-pattern is the same as working-pattern, but one can overrride the
 * {@link getTestPattern} to accomodate for differences, if desired.
 * 
 * Implementation for {@link #getSubEntry getSubEntry} provides a mechanism
 * for creating a subtitle record when the data-input matched a working
 * pattern, and the implementation of 
 * {@link #appendSubEntry appendSubEntry} allows the record to be written
 * to a string buffer for writing to a file.
 * 
 * This model suited simple subtitle format, where pattern of 
 * subtitle entries repeats. It does allow extensions of parsing other 
 * components in the {@link #initLoader initLoader} and writing them 
 * out in {@link #initSaver initSaver}
 * 
 * @author teras
 */
public abstract class AbstractTextSubFormat extends SubFormat {
    
    protected static final String nl_repeat = "[\\n]{2,}+"; //at least twice or more
    protected static final String nl = "\\\n";
    protected static final String sp = "[ \\t]*";
    
    protected Subtitles subtitle_list;
    
    /* Initialization functions */
    private void setFPS(float FPS) { this.FPS=FPS; }
    
    /* Loading functions */
    protected abstract SubEntry getSubEntry(Matcher m);
    
    protected abstract Pattern getPattern();
    
    /* Saving functions */
    protected abstract void appendSubEntry(SubEntry sub, StringBuffer str);
    
    protected Pattern getTestPattern() {
        return getPattern();
    }
    
    public Subtitles parse(String input, float FPS, File f) {
        try{
            if ( ! getTestPattern().matcher(input).find() ) return null;    // Not valid - test pattern does not match
            
            DEBUG.debug(_("Found file {0}", _(getExtendedName())));
            subtitle_list = new Subtitles();
            setFPS(FPS);
            input = initLoader(input);
            SubAttribs attr = subtitle_list.getAttribs();   // This method should be called after initLoader()
            
            Matcher m = getPattern().matcher(input);
            SubEntry entry;
            while(m.find()){
                entry = getSubEntry(m);
                if (entry !=null) {
                    entry.updateMaxCharStatus(attr, entry.getMetrics().maxlength);
                    subtitle_list.add(entry);
                }
            }
            if ( subtitle_list.isEmpty()) return null;
            return subtitle_list;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    protected String initLoader(String input) { return input+"\n"; }
    protected void cleanupLoader(Subtitles sub) {}
    
    
    public boolean produce(Subtitles subs, File outfile, MediaFile media) throws IOException {
        StringBuffer res = new StringBuffer();
        initSaver(subs, media, res);
        for ( int i = 0 ; i < subs.size() ; i++ ) {
            appendSubEntry(subs.elementAt(i), res);
        }
        cleanupSaver(res);
        
        /* Clean up leading \n characters */
        while (res.charAt(res.length()-1)=='\n' && res.charAt(res.length()-2)=='\n') {
            res.setLength(res.length()-1);
        }
        
        // encoder = Charset.forName(jub.prefs.getSaveEncoding()).newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        CharsetEncoder encoder = Charset.forName(getEncoding()).newEncoder();
        
        BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(outfile), encoder));
        out.write(res.toString().replace("\n","\r\n"));
        out.close();
        return true;
    }
    
    protected void initSaver(Subtitles subs, MediaFile media, StringBuffer header) {}
    protected void cleanupSaver(StringBuffer footer) {}
    
    protected void updateAttributes(String input, Pattern title, Pattern author, Pattern source, Pattern comments) {
        Matcher m;
        String attrs[] = new String[4];
        
        m = title.matcher(input);
        if (m.find()) attrs[0] =m.group(1).trim();
        
        m = author.matcher(input);
        if (m.find()) attrs[1] = m.group(1).trim();
        
        m = source.matcher(input);
        if (m.find()) attrs[2] = m.group(1).trim();
        
        m = comments.matcher(input);
        StringBuffer com_b = new StringBuffer();
        while (m.find()) {
            if (!(m.start()!=0 && input.charAt(m.start()-1)!='\n'))
                com_b.append(m.group(1).trim()).append('\n');
        }
        String com = com_b.toString().replace('|', '\n');
        if (com.length() > 0 ) attrs[3] = com.substring(0, com.length()-1);
        
        for (int i = 0 ; i < attrs.length ; i++) {
            if (attrs[i] != null && attrs[i].equals(""))
                attrs[i] = null;
        }
        
        subtitle_list.setAttribs(new SubAttribs(attrs[0], attrs[1], attrs[2], attrs[3]));
    }
}
