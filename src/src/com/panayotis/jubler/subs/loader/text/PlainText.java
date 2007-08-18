/*
 * PlainText.java
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Subtitles;


/**
 *
 * @author teras
 */
public class PlainText extends AbstractTextSubFormat {
    
    private static final Pattern pat;
    
    private double current_time = 0;
    
    static {
        pat = Pattern.compile("(.*?)"+nl);
    }
    
    protected Pattern getPattern () {
        return pat;
    }
    
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(current_time);
        current_time += 2;
        Time finish = new Time(current_time);
        current_time += 1;
        return new SubEntry (start, finish, m.group(1));
    }
    
    
    public String getExtension() {
        return "txt";
    }
    
    public String getName() {
        return "PlainTxt";
    }
    
    public String getExtendedName() {
        return _("Plain text");
    }
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        str.append(sub.getText()).append('\n');
    }
    
    
    protected String initLoader(String input) {
        current_time = 0;
        return super.initLoader(input);
    }

    public boolean supportsFPS() { return false; }
}