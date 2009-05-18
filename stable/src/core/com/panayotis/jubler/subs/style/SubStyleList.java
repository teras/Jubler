/*
 * SubStyleList.java
 *
 * Created on 1 Σεπτέμβριος 2005, 11:56 πμ
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

package com.panayotis.jubler.subs.style;

import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.subs.SubEntry;

import java.util.Vector;

/**
 *
 * @author teras
 */
public class SubStyleList extends Vector<SubStyle> implements NameList {
    
        private static final SubStyle default_style;

        static {
            default_style = new SubStyle("Default");
            default_style.setDefault(true);
            default_style.setValues(Options.getOption("Styles.Default", ""));
        }

    
    /** Creates a new instance of SubStyleList */
    public SubStyleList() {
        add(new SubStyle(default_style));
        elementAt(0).setDefault(true);
    }
    
    
    public SubStyleList(SubStyleList old) {
        for (int i = 0 ; i < old.size() ; i++) {
            add(new SubStyle(old.elementAt(i)));
        }
        elementAt(0).setDefault(true);
    }
    
    public String getNameAt(int i) {
        return elementAt(i).Name;
    }
    
    public int getStyleIndex(SubEntry entry) {
        SubStyle style = entry.getStyle();
        int res;
        if (style == null || (res = indexOf(style)) < 0 ) {
            entry.setStyle(elementAt(0));
            return 0;
        }
        return res;
    }
    
    public int findStyleIndex(String name) {
        for (int i = 0 ; i < size() ; i++ ) {
            if ( name.equals(elementAt(i).Name) ) {
                return i;
            }
        }
        return 0;
    }
    
    public SubStyle getStyleByName(String name) {
        return elementAt(findStyleIndex(name));
    }
    
    public SubStyle clearList() {
        SubStyle d = elementAt(0);
        removeAllElements();
        return d;
    }
    
}