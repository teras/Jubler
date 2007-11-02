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

package com.panayotis.jubler.subs.loader.text;

import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;
import static com.panayotis.jubler.subs.style.StyleType.*;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.subs.loader.text.format.StyledFormat;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public class AdvancedSubStation extends SubStationAlpha {
    
    private static final Pattern testpat, styles;
    private static final Vector<StyledFormat> styles_dict;
    private static final HashMap<String, Direction> ass_directions;
    
    /** Creates a new instance of SubFormat */
    static {
        testpat = Pattern.compile("(?i)(?s)\\[Script Info\\].*?\\[v4(\\+ Styles)|( Styles\\+)\\].*?"
                + "Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?"+nl
                );
        
        styles = Pattern.compile("(?i)Style:(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)"+nl);
        
        ass_directions=new HashMap<String, Direction>(9);
        ass_directions.put("8", TOP);
        ass_directions.put("9", TOPRIGHT);
        ass_directions.put("6", RIGHT);
        ass_directions.put("3", BOTTOMRIGHT);
        ass_directions.put("2", BOTTOM);
        ass_directions.put("1", BOTTOMLEFT);
        ass_directions.put("4", LEFT);
        ass_directions.put("7", TOPLEFT);
        ass_directions.put("5", CENTER);
        
        styles_dict=new Vector<StyledFormat>();
        styles_dict.add(new StyledFormat(ITALIC, "i0", false));
        styles_dict.add(new StyledFormat(ITALIC, "i1", true));
        styles_dict.add(new StyledFormat(BOLD, "b0", false));
        styles_dict.add(new StyledFormat(BOLD, "b1", true));
        styles_dict.add(new StyledFormat(UNDERLINE, "u0", false));
        styles_dict.add(new StyledFormat(UNDERLINE, "u1", true));
        styles_dict.add(new StyledFormat(STRIKETHROUGH, "s0", false));
        styles_dict.add(new StyledFormat(STRIKETHROUGH, "s1", true));
        
        /* Use these entries to grab a priori unsupported special tags
         * We need these functions since tags are identified with a .startsWith() method;
         * It *has* to be un-storable, in order to be stored only *once*, 
         *   in the last wildcard entry (UNKNOWN, "", null)
         */
        styles_dict.add(new StyledFormat(UNKNOWN, "fsc", null, false));
        styles_dict.add(new StyledFormat(UNKNOWN, "fsp", null, false));
        
        /* Remaining tags */
        styles_dict.add(new StyledFormat(FONTNAME, "fn", null));
        styles_dict.add(new StyledFormat(FONTSIZE, "fs", null));
        styles_dict.add(new StyledFormat(PRIMARY, "1c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(PRIMARY, "c",     StyledFormat.COLOR_REVERSE, false));
        styles_dict.add(new StyledFormat(PRIMARY, "1a", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(PRIMARY, "alpha", StyledFormat.COLOR_ALPHA_REVERSE, false));
        styles_dict.add(new StyledFormat(SECONDARY, "2c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(SECONDARY, "2a", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(OUTLINE, "3c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(OUTLINE, "3a", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(SHADOW, "4c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(SHADOW, "4a", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(DIRECTION, "an", ass_directions));
        styles_dict.add(new StyledFormat(DIRECTION, "a", ssa_directions, false));
        styles_dict.add(new StyledFormat(UNKNOWN, "", null));   // Add this line if you want this style to save unknwn formats. It has to be LAST since it matches ALL tags
    }
    
    protected Pattern getTestPattern() { return testpat; }
    protected Vector<StyledFormat> getStylesDictionary() { return styles_dict; }
    
    public String getExtension() { return "ass"; }
    public String getName() { return "AdvancedSubStation"; }
    
    
    
    protected String getExtraVersion() { return "+"; }
    protected String getLayerTitle() { return "Layer"; }
    
    
    protected void appendStyles(Subtitles subs, StringBuffer header) {
        header.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour," +
                " Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        for (SubStyle style : subs.getStyleList()) {
            header.append("Style: ");
            header.append(style.Name).append(',');
            header.append(style.get(FONTNAME)).append(',');
            header.append( Math.round(((Number)style.get(FONTSIZE)).doubleValue() * getFontFactor()) ).append(',');
            header.append(AlphaColorToString(style.get(PRIMARY), true)).append(',');
            header.append(AlphaColorToString(style.get(SECONDARY), true)).append(',');
            header.append(AlphaColorToString(style.get(OUTLINE), true)).append(',');
            header.append(AlphaColorToString(style.get(SHADOW), true)).append(',');
            header.append(booleanToInt(style.get(BOLD))).append(',');
            header.append(booleanToInt(style.get(ITALIC))).append(',');
            header.append(booleanToInt(style.get(UNDERLINE))).append(',');
            header.append(booleanToInt(style.get(STRIKETHROUGH))).append(',');
            header.append(XSCALE.get(style)).append(',');
            header.append(YSCALE.get(style)).append(',');
            header.append(SPACING.get(style)).append(',');
            header.append(ANGLE.get(style)).append(',');
            header.append((((Number)style.get(BORDERSTYLE)).intValue()==0)?1:3).append(',');
            header.append(BORDERSIZE.get(style)).append(',');
            header.append(SHADOWSIZE.get(style)).append(',');
            header.append(getDirectionKey(ass_directions, (Direction)style.get(DIRECTION))).append(',');
            header.append(LEFTMARGIN.get(style)).append(',');
            header.append(RIGHTMARGIN.get(style)).append(',');
            header.append(VERTICAL.get(style)).append(',');
            header.append(0).append('\n');
        }
        
    }
    
    
    protected void getStyles(String input) {
        Matcher m = styles.matcher(input);
        SubStyleList list = subtitle_list.getStyleList();
        SubStyle deflt = list.clearList();
        
        SubStyle st;
        AlphaColor pri;
        while (m.find()) {
            st = new SubStyle(m.group(1).trim());
            st.set(FONTNAME, m.group(2));
            st.set(FONTSIZE, Math.round( Integer.parseInt(m.group(3)) / getFontFactor()) );
            st.set(PRIMARY, StringToAlphaColor(m.group(4), null));
            st.set(SECONDARY, StringToAlphaColor(m.group(5), null));
            st.set(OUTLINE, StringToAlphaColor(m.group(6), null));
            st.set(SHADOW, StringToAlphaColor(m.group(7), null));
            st.set(BOLD, m.group(8));
            st.set(ITALIC, m.group(9));
            st.set(UNDERLINE, m.group(10));
            st.set(STRIKETHROUGH, m.group(11));
            st.set(XSCALE, m.group(12));
            st.set(YSCALE, m.group(13));
            st.set(SPACING, m.group(14));
            st.set(ANGLE, m.group(15));
            st.set(BORDERSTYLE, (m.group(16).equals("3") ? 1 : 0) );
            st.set(BORDERSIZE, m.group(17));
            st.set(SHADOWSIZE, m.group(18));
            st.set(DIRECTION, ass_directions.get(m.group(19)));
            st.set(LEFTMARGIN, m.group(20));
            st.set(RIGHTMARGIN, m.group(21));
            st.set(VERTICAL, m.group(22));
            
            list.add(st);
        }
        if (list.size()==0) list.add(deflt);
    }
    
}
