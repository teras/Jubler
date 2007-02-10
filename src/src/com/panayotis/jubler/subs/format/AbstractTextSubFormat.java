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

package com.panayotis.jubler.subs.format;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.subs.Subtitles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 *
 * @author teras
 */
public abstract class AbstractTextSubFormat extends SubFormat {
    
    protected static final String nl = "\\\n";
    protected static final String space = "\\s*?";
    protected float FPS  = 25f;
    
    protected Subtitles subtitle_list;
    
    /* Initialization functions */
    protected String initLoader(String input, Subtitles subs) { return input+"\n"; }
    private void setFPS(float FPS) { this.FPS=FPS; }
    
    /* Loading functions */
    protected abstract SubEntry getSubEntry(Matcher m);
    
    protected abstract Pattern getPattern();
    
    /* Saving functions */
    protected abstract String makeSubEntry(SubEntry sub);
    
    protected Pattern getTestPattern() {
        return getPattern();
    }
    
    
    
    public Subtitles parse(String input, float FPS, File f) {
        Time start, finish;
        String sub;
        
        try{
            if ( ! getTestPattern().matcher(input).find() ) return null;    // Not valid - test pattern does not match
            
            DEBUG.info(_("Found file {0}", _(getExtendedName())));
            subtitle_list = new Subtitles();
            setFPS(FPS);
            input = initLoader(input, subtitle_list);
            Matcher m = getPattern().matcher(input);
            SubEntry entry;
            while(m.find()){
                subtitle_list.add(getSubEntry(m));
            }
            if ( subtitle_list.isEmpty()) return null;
            return subtitle_list;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    public void produce(Subtitles subs, File outfile, JPreferences prefs, MediaFile media) throws IOException {
        CharsetEncoder encoder;
        String encoding;
        
        StringBuffer res = new StringBuffer();
        float fps = getFPS(prefs);
        
        if ( prefs == null ) encoding = "UTF-8";
        else encoding = prefs.getSaveEncoding();
        
        // encoder = Charset.forName(jub.prefs.getSaveEncoding()).newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        encoder = Charset.forName(encoding).newEncoder();
        
        BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(outfile), encoder));
        
        setFPS(FPS);
        res.append(makeHeader(subs));
        for ( int i = 0 ; i < subs.size() ; i++ ) {
            res.append(makeSubEntry(subs.elementAt(i)));
        }
        out.write(res.toString().replace("\n","\r\n"));
        out.close();
    }
    
    protected String makeHeader(Subtitles subs) { return ""; }
}
