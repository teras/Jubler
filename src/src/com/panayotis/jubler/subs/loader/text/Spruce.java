/*
 * Spruce.java
 *
 * Created on 24 April 2007, 11:27 am
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


/**
 *
 * @author teras
 */
public class Spruce extends AbstractTextSubFormat {
    
    private static final Pattern pat;
    
    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)\\s*,\\s*(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)\\s*,\\s*(.*?)"+nl);
    }
    
    protected Pattern getPattern() {
        return pat;
    }
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), m.group(2), m.group(3), m.group(4), FPS);
        Time finish = new Time(m.group(5), m.group(6), m.group(7), m.group(8), FPS);
        return new SubEntry(start, finish, m.group(9).replace("|","\n"));
    }
    
    public String getExtension() {
        return "stl";
    }
    
    public String getName() {
        return "Spruce";
    }
    
    public String getExtendedName() {
        return "Spruce DVDMaestro";
    }
    
    protected String makeSubEntry(SubEntry sub){
        StringBuffer res;
        String time;
        
        res = new StringBuffer();
        
        time = sub.getStartTime().toString().replace(',',':');
        res.append(sub.getStartTime().toSecondsFrame(FPS));
        res.append(" , ");
        res.append(sub.getFinishTime().toSecondsFrame(FPS));
        res.append(" , ");
        res.append(sub.getText().replace('\n','|'));
        res.append("\n");
        return res.toString();
    }
    
    public boolean supportsFPS() {
        return true;
    }
    
}
