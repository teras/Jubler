/*
 * SubStationAlpha.java
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

import com.panayotis.jubler.subs.loader.text.format.StyledFormat;
import com.panayotis.jubler.subs.loader.text.format.StyledTextSubFormat;
import static com.panayotis.jubler.subs.style.StyleType.*;
import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.util.HashMap;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class SubStationAlpha extends StyledTextSubFormat {
    
    private static final Pattern pat, testpat;
    
    private static final Pattern title, author, source, comments, styles, stylepattern;
    
    private static final Vector<StyledFormat> styles_dict;
    protected static final HashMap<String, Direction> ssa_directions;
    
    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile(
                /* We ignore the Marked option */
                "(?i)Dialogue:(.*?),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)"+nl
                );
        
        testpat = Pattern.compile("(?i)(?s)\\[Script Info\\].*?\\[V4 Styles\\].*?"
                + "Dialogue:.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?"+nl
                );
        
        title = Pattern.compile("(?i)Title:" + sp + "(.*?)"+nl);
        author = Pattern.compile("(?i)Original Script:" + sp + "(.*?)"+nl);
        source = Pattern.compile("(?i)Update Details:" + sp + "(.*?)"+nl);
        comments = Pattern.compile(";(.*?)"+nl);
        styles = Pattern.compile("(?i)Style:(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?),(.*?)"+nl);
        
        stylepattern = Pattern.compile("\\{(.*?)\\}");
        
        ssa_directions=new HashMap<String, Direction>(9);
        ssa_directions.put("6", TOP);
        ssa_directions.put("7", TOPRIGHT);
        ssa_directions.put("11", RIGHT);
        ssa_directions.put("3", BOTTOMRIGHT);
        ssa_directions.put("2", BOTTOM);
        ssa_directions.put("1", BOTTOMLEFT);
        ssa_directions.put("9", LEFT);
        ssa_directions.put("5", TOPLEFT);
        ssa_directions.put("10", CENTER);
        
        styles_dict=new Vector<StyledFormat>();
        styles_dict.add(new StyledFormat(ITALIC, "i0", false));
        styles_dict.add(new StyledFormat(ITALIC, "i1", true));
        styles_dict.add(new StyledFormat(BOLD, "b0", false));
        styles_dict.add(new StyledFormat(BOLD, "b1", true));
        styles_dict.add(new StyledFormat(FONTNAME, "fn", null));
        styles_dict.add(new StyledFormat(FONTSIZE, "fs", null));
        styles_dict.add(new StyledFormat(PRIMARY, "c", StyledFormat.COLOR_REVERSE));
        styles_dict.add(new StyledFormat(PRIMARY, "alpha", StyledFormat.COLOR_ALPHA_REVERSE));
        styles_dict.add(new StyledFormat(DIRECTION, "a", ssa_directions));
        
        styles_dict.add(new StyledFormat(UNKNOWN, "", null));   // Add this line if you want this style to save unknwn formats. It has to be LAST since it matches ALL tags
    }
    
    
    protected Pattern getPattern() { return pat; }
    protected Pattern getTestPattern() { return testpat; }
    
    protected Pattern getStylePattern() { return stylepattern; }
    protected String getTokenizer() { return "\\"; }
    protected String getEventIntro() { return "{"; }
    protected String getEventFinal() { return "}"; }
    protected String getEventMark() { return getTokenizer(); }
    protected boolean isEventCompact() { return true; }
    
    protected float getFontFactor() { return 1.3f; }
    
    protected Vector<StyledFormat> getStylesDictionary() { return styles_dict; }
    
    public boolean supportsFPS() { return false; }
    
    public String getExtension() { return "ssa"; }
    public String getName() { return "SubStationAlpha"; }
    
    
    
    protected String initLoader(String input) {
        input = super.initLoader(input);
        getStyles(input);
        updateAttributes(input, title, author, source, comments);
        return input;
    }
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        SubEntry entry = new SubEntry(start, finish, m.group(16).replace("\\N", "\n").replace("\\n", "\n"));
        entry.setStyle(subtitle_list.getStyleList().getStyleByName(m.group(10)));
        parseSubText(entry);
        
        // m.group(1) is still unusable
        // m.group(11-15) are still unusable
        return entry;
    }
    
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        str.append("Dialogue: ");
        str.append("0").append(',');    // Layer - Marked
        str.append(timeformat(sub.getStartTime()));
        str.append(',');
        str.append(timeformat(sub.getFinishTime())).append(',');
        if (sub.getStyle() == null ) {
            str.append("*Default");
        } else {
            if ( sub.getStyle().isDefault()) str.append('*');
            str.append(sub.getStyle().Name);
        }
        str.append(",,0000,0000,0000,,");
        str.append(rebuildSubText(sub).replace("\n", "\\N"));
        str.append("\n");
    }
    
    private String timeformat(Time t) {
        String res = t.getSeconds().substring(1).replace(',','.');
        res = res.substring(0, res.length()-1);
        return res;
    }
    
    
    
    protected void initSaver(Subtitles subs, MediaFile media, StringBuffer header) {
        
        header.append("[Script Info]\n");
        
        SubAttribs attr = subs.getAttribs();
        String com = attr.getComments();
        if (!com.trim().equals("")) {
            com = com.replace("\n", "\n; ");
            header.append("; ");
            header.append(com);
            header.append('\n');
        }
        
        header.append("Title: ").append(attr.getTitle());
        header.append("\nOriginal Script: ").append(attr.getAuthor());
        header.append("\nUpdate Details: ").append(attr.getSource());
        header.append("\nScriptType: v4.00").append(getExtraVersion());
        header.append("\nCollisions: Normal\n");
        
        if (media !=null && media.getVideoFile()!=null) {
            header.append("PlayResX: ").append(media.getVideoFile().getWidth());
            header.append("\nPlayResY: ").append(media.getVideoFile().getHeight()).append('\n');
        }
        
        header.append("PlayDepth: 0\nTimer: 100,0000\n");
        
        header.append("\n[V4");
        header.append(getExtraVersion());
        header.append(" Styles]\n");
        appendStyles(subs, header);
        
        header.append("\n[Events]\nFormat: ");
        header.append(getLayerTitle());
        header.append(", Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
    }
    
    
    protected String getExtraVersion() { return ""; }
    protected String getLayerTitle() { return "Marked"; }
    
    protected int booleanToInt(Object b) {
        return (((Boolean)b).booleanValue())?-1:0;
    }
    
    
    
    
    protected void appendStyles(Subtitles subs, StringBuffer header) {
        header.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding\n");
        for (SubStyle style : subs.getStyleList()) {
            header.append("Style: ");
            header.append(style.Name).append(',');
            header.append(style.get(FONTNAME)).append(',');
            header.append( Math.round(((Integer)style.get(FONTSIZE)) * getFontFactor()) ).append(',');
            header.append(AlphaColorToString(style.get(PRIMARY), false)).append(',');
            header.append(AlphaColorToString(style.get(SECONDARY), false)).append(',');
            header.append(AlphaColorToString(style.get(OUTLINE), false)).append(',');
            header.append(AlphaColorToString(style.get(SHADOW), false)).append(',');
            header.append(booleanToInt(style.get(BOLD))).append(',');
            header.append(booleanToInt(style.get(ITALIC))).append(',');
            header.append((((Integer)style.get(BORDERSTYLE)).intValue()==0)?1:3).append(',');
            header.append(BORDERSIZE.get(style)).append(',');
            header.append(SHADOWSIZE.get(style)).append(',');
            header.append(getDirectionKey(ssa_directions, (Direction)style.get(DIRECTION))).append(',');
            header.append(LEFTMARGIN.get(style)).append(',');
            header.append(RIGHTMARGIN.get(style)).append(',');
            header.append(VERTICAL.get(style)).append(',');
            header.append(((AlphaColor)style.get(PRIMARY)).getAlpha()).append(',');
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
            st.set(PRIMARY, StringToAlphaColor(m.group(4), m.group(17)));
            st.set(SECONDARY, StringToAlphaColor(m.group(5), m.group(17)));
            st.set(OUTLINE, StringToAlphaColor(m.group(6), m.group(17)));
            st.set(SHADOW, StringToAlphaColor(m.group(7), null));
            st.set(BOLD, m.group(8));
            st.set(ITALIC, m.group(9));
            st.set(BORDERSTYLE, (m.group(10).equals("3") ? 1 : 0) );
            st.set(BORDERSIZE, m.group(11));
            st.set(SHADOWSIZE, m.group(12));
            st.set(DIRECTION, ssa_directions.get(m.group(13)));
            st.set(LEFTMARGIN, m.group(14));
            st.set(RIGHTMARGIN, m.group(15));
            st.set(VERTICAL, m.group(16));
            
            list.add(st);
        }
        if (list.size()==0) list.add(deflt);
    }
    
    
    /* If the Alpha channel is stored in the BGR, then the Alpha parameter should be NULL */
    protected AlphaColor StringToAlphaColor(String revRGB, String Alpha ) {
        long lrgb = parseNumber(revRGB);
        int rgb = (int)(lrgb & 0xffffff);
        int alpha = ((int)lrgb&0xff000000) >> 24;
        if (Alpha!=null) alpha = (int)parseNumber(Alpha);
        alpha = invertAlpha(alpha) << 24;
        return new AlphaColor( alpha | reverseByteOrder(rgb) );
    }
    protected String AlphaColorToString(Object acolor, boolean store_alpha) {
        long rgb = reverseByteOrder(((AlphaColor)acolor).getRGB() & 0xffffff);
        long alpha = store_alpha ? ((long)invertAlpha(((AlphaColor)acolor).getAlpha())) << 24 : 0;
        int length = store_alpha ? 8 : 6;
        return produceHexNumber( alpha|rgb, false, length);
    }
    
}
