/*
 * MicroDVD.java
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


/**
 *
 * @author teras
 */
public class MicroDVD extends AbstractTextSubFormat {
    
    private static final Pattern pat;
    
    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile("\\{(\\d+)\\}"+sp+"\\{(\\d+)\\}(.*?)"+nl);
    }
    
    protected Pattern getPattern() {
        return pat;
    }
    

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), FPS);
        Time finish = new Time(m.group(2), FPS);
        return new SubEntry (start, finish, m.group(3).replace("|","\n"));
    }
    
    
    public String getExtension() {
        return "sub";
    }
    
    public String getName() {
        return "MicroDVD";
    }
     
    public String getExtendedName() {
        return "MicroDVD SUB file";
    } 
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        str.append("{");
        str.append(sub.getStartTime().getFrames(FPS));
        str.append("}{");
        str.append(sub.getFinishTime().getFrames(FPS));
        str.append("}");
        str.append(sub.getText().replace('\n','|'));
        str.append("\n");
    }

    public boolean supportsFPS() { return true; }
    
}
