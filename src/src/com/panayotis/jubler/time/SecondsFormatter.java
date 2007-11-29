/*
 * TimeSpinnerFormatter.java
 *
 * Created on 23 Ιούνιος 2005, 3:23 πμ
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

package com.panayotis.jubler.time;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.MaskFormatter;

/**
 *
 * @author teras
 */
public class SecondsFormatter extends MaskFormatter {
    private static Pattern pat;
    
    static {
        pat = Pattern.compile("(\\d+):(\\d+):(\\d+),(\\d\\d\\d)\\d*");
    }
    
    public SecondsFormatter () throws ParseException { 
        super("##:##:##,###");
        setPlaceholder(null);
        setPlaceholderCharacter('0');
    }
    
    public Object stringToValue(String text) throws ParseException {
        Matcher m = pat.matcher(text);
        if ( !m.matches()) {
            throw new ParseException("",0);
        }
        Time res = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        return res;
    }
        
    public String valueToString(Object value) {
        return ((Time)value).getSeconds();
    }
}
