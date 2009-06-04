/*
 * SubStyle.java
 *
 * Created on 1 Σεπτέμβριος 2005, 11:37 πμ
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

import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public class SubStyle implements Comparable {
    
    
    public static enum Direction {TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT, TOPLEFT, CENTER};
    
    /* since this is stored already in an array, we will not use StyleType.init() */
    public static final Integer [] FontSizes = {8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 26, 28, 32, 36, 40, 48, 56, 64, 72};
    
    public static final String [] FontNames;
    
    private static final Pattern loadpattern;
    
    static {
        loadpattern = Pattern.compile(
                "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"+
                "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"+
                "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"+
                "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"+
                "(.*)"
                );
        
        /* Load font names in a safe manner */
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fnames;
        try {
            fnames = env.getAvailableFontFamilyNames();
        } catch (Exception e1) {
            DEBUG.debug("Using failsafe routine for font loading.");
            Font[] fnt = env.getAllFonts();
            TreeSet<String> names = new TreeSet<String>();
            
            for (int i = 0 ; i < fnt.length ; i++ ) {
                try {
                    names.add(fnt[i].getFamily());
                } catch (Exception e2) {}
            }
            
            String[] model = new String[1];
            fnames = names.toArray(model);
        }
        FontNames = fnames;
        
    }
    
    
    public String Name;
    private Object [] values;
    private boolean isDefault = false;
    
    
    public SubStyle(SubStyle old) {
        setValues(old);
    }
    
    /** Creates a new instance of SubStyle */
    public SubStyle(String name) {
        this.Name = name;
        
        /* Initialize default values */
        StyleType[] types = StyleType.values();
        values = new Object[types.length];
        
        for (int i = 0 ; i < types.length ; i++) {
            values[i] = types[i].getDefault();
        }
    }
    
    public void setName(String newname, SubStyleList list) {
        UniqName uniq = new UniqName(newname);
        Name = uniq.getUniqName(list, this);
    }
    public String getName() {
        return Name;
    }
    
    
    public void setValues(SubStyle old) {
        Name = old.Name;
        values = new Object[StyleType.values().length];
        for (int i = 0 ; i < values.length ; i++) {
            values[i] = old.values[i];
        }
    }
    
    
    public void setValues(String newvalues) {
        if (newvalues==null) return;
        Matcher m = loadpattern.matcher(newvalues);
        if (!m.find()) return;
        
        String current;
        for (int i = 0 ; i < values.length-1 ; i++ ) {  // Ignore last "unknown" event
            current = m.group(i+1);
            values[i] = StyleType.values()[i].init(current);
        }
    }
    
    
    public String getValues() {
        StringBuffer out = new StringBuffer();
        
        for (int i = 0 ; i < values.length-1 ; i++ ) {  // Ignore last "unknown" event
            out.append('|').append(values[i]);
        }
        return out.substring(1);
    }
    
    public Object get(StyleType which) { return values[which.ordinal()]; }
    public Object get(int which) { return values[which]; }
    
    public void set(StyleType which, Object what) {
        if (what==null) {
            DEBUG.debug(_("Null value found while setting Style {0} - ignoring.", which.name()));
            return;
        }
        int where = which.ordinal();
        /* Overloading doesn't really work well in this case, so we have to force it */
        if (what instanceof String)
            values[where] = which.init((String)what);
        else
            values[where] = which.init(what);
    }
    
    
    
    public String toString() { return getName(); }
    
    public void setDefault(boolean def) { isDefault = def; }
    public boolean isDefault() { return isDefault; }
    
    
    
    
    class UniqName {
        
        private String text_name = "";
        private int numb_name = 1;
        private String newname = "";
        
        public UniqName(String name) {
            newname = name;
            int split = name.length()-1;
            /* Go on and on, until we find a non-numeric character */
            while ( split >= 0 && Character.isDigit(name.charAt(split)) ) split--;
            /* Get textual part of the name */
            text_name = name.substring(0, split+1);
            
            /* Get numeric part of the name */
            try {
                numb_name = Integer.parseInt(name.substring(split+1));
            } catch (NumberFormatException e) {
                numb_name = 1;
            }
            
            /* keep default value, if no numeric part is found */
            if  ( numb_name < 0 ) numb_name = 1;
        }
        
        
        /* Check if the name already exists */
        private boolean findNameInList(NameList list, Object obj) {
            for (int i = 0 ; i < list.size() ; i++ ) {
                if (list.elementAt(i)!=obj && list.getNameAt(i).equals(newname)) return true;
            }
            return false;
        }
        
        private final void normalizeInternalName(NameList list, Object obj) {
        /* go through all objects of the list and find the one with the maximum
         * value AND same base text */
            UniqName other;
            for (int i = 0 ; i < list.size() ; i++ ) {
                /* Split names of list element in two (again) */
                other = new UniqName(list.getNameAt(i));
                if ( list.elementAt(i)!=obj && other.numb_name >= numb_name && other.text_name.equals(text_name) ){
                    /* update number */
                    numb_name = other.numb_name+1;
                }
            }
        }
        
        public String getUniqName(NameList list, Object obj) {
            if (findNameInList(list, obj)) {
                normalizeInternalName(list, obj);
                return text_name+numb_name;
            }
            return newname;
        }
    }

    public int compareTo(Object o) {
        return Name.compareTo( ((SubStyle)o).Name );
    }
}