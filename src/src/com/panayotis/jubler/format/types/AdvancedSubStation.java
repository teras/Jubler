/*
 * AdvancedSubStation.java
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

package com.panayotis.jubler.format.types;
import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;
import static com.panayotis.jubler.subs.style.SubStyle.Style.*;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.StyleConstants;




/**
 *
 * @author teras
 */
public class AdvancedSubStation extends SubStationAlpha {
    
    private static final Pattern testpat, styles;
    
    /** Creates a new instance of SubFormat */
    static {
        testpat = Pattern.compile("(?i)(?s)\\[Script Info\\].*?\\[v4 Styles\\+\\].*?"
                + "Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?"+nl
                );
        
        styles = Pattern.compile("(?i)Style:(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)"+nl);
    }
    
    protected Pattern getTestPattern() {
        return testpat;
    }
    
    
    public String getExtension() {
        return "ass";
    }
    
    public String getName() {
        return "AdvancedSubStation";
    }
    
    protected String getExtraVersion() { return "+"; }
    protected String getLayer() { return "0"; }
    
    protected int directionToInt(Direction dir) {
        switch (dir) {
            case TOP:
                return 8;
            case TOPRIGHT:
                return 9;
            case RIGHT:
                return 6;
            case BOTTOMRIGHT:
                return 3;
            case BOTTOMLEFT:
                return 1;
            case LEFT:
                return 4;
            case TOPLEFT:
                return 7;
            case CENTER:
                return 5;
        }
        /* By default return BOTTOM */
        return 2;
    }
    
    protected Direction intToDirection(int i) {
        switch (i) {
            case 8:
                return TOP;
            case 9:
                return TOPRIGHT;
            case 6:
                return RIGHT;
            case 3:
                return BOTTOMRIGHT;
            case 1:
                return BOTTOMLEFT;
            case 4:
                return LEFT;
            case 7:
                return TOPLEFT;
            case 5:
                return CENTER;
        }
        /* By default return bottom */
        return BOTTOM;
    }
    
    protected String getTagFromStyle(Object style) {
        if (style==StyleConstants.FontFamily) return "\\fn";
        else if (style==StyleConstants.FontSize) return "\\fs";
        else if (style==StyleConstants.Bold) return "\\b";
        else if (style==StyleConstants.Italic) return "\\i";
        else if (style==StyleConstants.Underline) return "\\u";
        else if (style==StyleConstants.StrikeThrough) return "\\s";
        else if (style==StyleConstants.Foreground) return "\\1c";
        else if (style.equals("secondary")) return "\\2c";
        else if (style.equals("outline")) return "\\3c";
        else if (style==StyleConstants.Background) return "\\4c";
        else if (style==StyleConstants.Alignment) return "\\a";
        else if (style.equals("unknown")) return "\\";
        return null;
    }
    
    
    protected void appendStyles(Subtitles subs, StringBuffer header) {
        header.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColor, BackColour," +
                " Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        for (SubStyle style : subs.getStyleList()) {
            header.append("Style: ");
            header.append(style.Name).append(',');
            header.append(style.get(FONTNAME)).append(',');
            header.append(style.get(FONTSIZE)).append(',');
            header.append(setReverse(style.get(PRIMARY), true)).append(',');
            header.append(setReverse(style.get(SECONDARY), true)).append(',');
            header.append(setReverse(style.get(OUTLINE), true)).append(',');
            header.append(setReverse(style.get(SHADOW), true)).append(',');
            header.append(booleanToInt(style.get(BOLD))).append(',');
            header.append(booleanToInt(style.get(ITALIC))).append(',');
            header.append(booleanToInt(style.get(UNDERLINE))).append(',');
            header.append(booleanToInt(style.get(STRIKETHROUGH))).append(',');
            header.append(style.get(XSCALE)).append(',');
            header.append(style.get(YSCALE)).append(',');
            header.append(style.get(SPACING)).append(',');
            header.append(style.get(ANGLE)).append(',');
            header.append((((Integer)style.get(BORDERSTYLE)).intValue()==0)?1:3).append(',');
            header.append(style.get(BORDERSIZE)).append(',');
            header.append(style.get(SHADOWSIZE)).append(',');
            header.append(directionToInt((Direction)style.get(DIRECTION))).append(',');
            header.append(style.get(LEFTMARGIN)).append(',');
            header.append(style.get(RIGHTMARGIN)).append(',');
            header.append(style.get(VERTICAL)).append(',');
            header.append(0).append('\n');
        }
        
    }
    
    
    protected void getStyles(String input, Subtitles subs) {
        Matcher m = styles.matcher(input);
        SubStyleList list = subs.getStyleList();
        list.clearList();
        
        SubStyle st;
        AlphaColor pri;
        while (m.find()) {
            st = new SubStyle(m.group(1).trim());
            st.set(FONTNAME, m.group(2));
            st.set(FONTSIZE, new Integer(m.group(3)));
            st.set(PRIMARY, getReverse(m.group(4), null));
            st.set(SECONDARY, getReverse(m.group(5), null));
            st.set(OUTLINE, getReverse(m.group(6), null));
            st.set(SHADOW, getReverse(m.group(7), null));
            st.set(BOLD, strToBoolean(m.group(8)));
            st.set(ITALIC, strToBoolean(m.group(9)));
            st.set(UNDERLINE, strToBoolean(m.group(10)));
            st.set(STRIKETHROUGH, strToBoolean(m.group(11)));
            st.set(XSCALE, new Integer(m.group(12)));
            st.set(YSCALE, new Integer(m.group(13)));
            st.set(SPACING, new Integer(m.group(14)));
            st.set(ANGLE, new Integer(m.group(15)));
            st.set(BORDERSTYLE, (m.group(16).equals("1"))? 0 : 1);
            st.set(BORDERSIZE, new Integer(m.group(17)));
            st.set(SHADOWSIZE, new Integer(m.group(18)));
            st.set(DIRECTION, intToDirection(Integer.parseInt(m.group(19))));
            st.set(LEFTMARGIN, new Integer(m.group(20)));
            st.set(RIGHTMARGIN, new Integer(m.group(21)));
            st.set(VERTICAL, new Integer(m.group(22)));
            
            if (st.Name.equals("Default")) {
                list.elementAt(0).setValues(st);
            } else {
                list.add(st);
            }
        }
    }
    
}
