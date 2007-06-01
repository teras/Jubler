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
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public class SubStyle {
    
    
    public static enum Direction {TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT, TOPLEFT, CENTER}
    
    public static enum Style {FONTNAME, FONTSIZE, BOLD, ITALIC, UNDERLINE, STRIKETHROUGH,
    PRIMARY, SECONDARY, OUTLINE, SHADOW, BORDERSTYLE, BORDERSIZE, SHADOWSIZE,
    LEFTMARGIN, RIGHTMARGIN, VERTICAL, ANGLE, SPACING, XSCALE, YSCALE, DIRECTION, UNKNOWN};
    
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
        
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fnames;
        try {
            fnames = env.getAvailableFontFamilyNames();
        } catch (Exception e1) {
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
        
        /* This is a prototype of the default style values */
        values = new Object[Style.values().length];
        values[Style.FONTNAME.ordinal()] = new String("Arial");
        values[Style.FONTSIZE.ordinal()] = new Integer(24);
        values[Style.BOLD.ordinal()] = new Boolean(false);
        values[Style.ITALIC.ordinal()] = new Boolean(false);
        values[Style.UNDERLINE.ordinal()] = new Boolean(false);
        values[Style.STRIKETHROUGH.ordinal()] = new Boolean(false);
        
        values[Style.PRIMARY.ordinal()] = new AlphaColor(Color.WHITE,  255);
        values[Style.SECONDARY.ordinal()] = new AlphaColor(Color.YELLOW,  255);
        values[Style.OUTLINE.ordinal()] = new AlphaColor(Color.BLACK,  180);
        values[Style.SHADOW.ordinal()] = new AlphaColor(Color.DARK_GRAY,  180);
        
        values[Style.BORDERSTYLE.ordinal()] = new Integer(0);
        values[Style.BORDERSIZE.ordinal()] = new Integer(2);
        values[Style.SHADOWSIZE.ordinal()] = new Integer(2);
        
        values[Style.LEFTMARGIN.ordinal()] = new Integer(20);
        values[Style.RIGHTMARGIN.ordinal()] = new Integer(20);
        values[Style.VERTICAL.ordinal()] = new Integer(20);
        
        values[Style.ANGLE.ordinal()] = new Integer(0);
        values[Style.SPACING.ordinal()] = new Integer(0);
        values[Style.XSCALE.ordinal()] = new Integer(100);
        values[Style.YSCALE.ordinal()] = new Integer(100);
        values[Style.DIRECTION.ordinal()] = Direction.BOTTOM;
        values[Style.UNKNOWN.ordinal()] = "";
    }
    
    public void setName(String newname, SubStyleList list) {
        
        UniqName uniq = new UniqName(newname);
        Name = uniq.getUniqName(list, this);
    }
    
    
    public void setValues(SubStyle old) {
        Name = old.Name;
        values = new Object[Style.values().length];
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
            try {
                if (values[i] instanceof String) values[i] = current;
                else if (values[i] instanceof Integer ) values[i] = new Integer(current);
                else if (values[i] instanceof Boolean ) values[i] = new Boolean(current);
                else if (values[i] instanceof AlphaColor ) values[i] = new AlphaColor(current);
                else if (values[i] instanceof Direction ) values[i] = Direction.valueOf(current);
                else DEBUG.error("UNKNOWN CLASS in SubStyle: "+values[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    
    public String getValues() {
        StringBuffer out = new StringBuffer();
        
        for (int i = 0 ; i < values.length-1 ; i++ ) {  // Ignore last "unknown" event
            out.append('|').append(values[i]);
        }
        return out.substring(1);
    }
    
    public Object get(Style which) { return values[which.ordinal()]; }
    public Object get(int which) { return values[which]; }
    
    public void set(Style which, Object what) {
        int where = which.ordinal();
        // if (values[where].getClass().getName().equals(what.getClass().getName()) ) {
        values[where] = what;
//            return;
//        }
//        DEBUG.error("Wrong cast in SubStyle set");
    }
    
    
    
    public String toString() { return Name; }
    
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
            String othername;
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
}