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

import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import com.sun.tools.doclets.internal.toolkit.util.Group;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;


/**
 *
 * @author teras
 */
public class Quicktime extends AbstractTextSubFormat {
    
    private static final Pattern pat, test_pat;
    
    private Time start, finish, mediafinish;
    String text;
    
    static {
        pat = Pattern.compile(
                "(?s)\\[(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)\\](.*?)((?=\\[)|\\z)"
                );
        test_pat = Pattern.compile("\\A\\{QTtext\\}");
    }
    
    protected Pattern getPattern() {
        return pat;
    }
    protected Pattern getTestPattern() {
        return test_pat;
    }
    
    
    public String getExtension() {
        return "txt";
    }
    
    public String getName() {
        return "Quicktime";
    }
    public String getExtendedName() {
        return _("Quicktime Texttrack");
    }
    
    protected String initLoader(String input) {
        start = null;
        return super.initLoader(input);
    }
    protected SubEntry getSubEntry(Matcher m) {
        /* Initialize matcher */
        if (start==null) {
            start = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
            text = m.group(5).replace('\n', ' ').trim();
            if (text.equals("")) text = null;
            return null;
        }
        /* Get current timing */
        finish = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        /* Prepare older subtitle */
        SubEntry ret = null;
        if (text!=null) ret = new SubEntry(start, finish, text);
        /* Prepare current subtitle */
        start = finish;
        text = m.group(5).replace('\n', ' ').trim();
        if (text.equals("")) text = null;
        return ret;
    }
    
    
    protected void initSaver(Subtitles subs, MediaFile media, StringBuffer header) {
        header.append("{QTtext}{timeScale:1000}{timeStamps:absolute}\n");
        start = null;
        finish = null;
        if (media.getVideoFile()!=null)
            mediafinish = new Time(media.getVideoFile().getLength());
        else mediafinish = new Time(0);
    }
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        if (start==null) {
            start = sub.getStartTime();
            if (start.toSeconds()>0d) {
                str.append("[00:00:00.000]\n");
            }
        }
        str.append('[');
        str.append(sub.getStartTime().toString().replace(',', '.'));
        str.append("]\n");
        str.append(sub.getText().replace('\n', ' ')).append('\n');
        finish = sub.getFinishTime();
    }
    protected void cleanupSaver(StringBuffer footer) {
        if (finish.compareTo(mediafinish)<0) finish = mediafinish;
        footer.append('[').append(finish.toString().replace(',', '.')).append("]\n");
    }
    
    public boolean supportsFPS() { return false; }
}
