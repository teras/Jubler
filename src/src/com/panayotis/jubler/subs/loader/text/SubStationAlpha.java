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

import static com.panayotis.jubler.subs.style.SubStyle.Style.*;
import static com.panayotis.jubler.subs.style.SubStyle.Direction.*;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;

import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.event.StyleoverEvent;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.text.StyleConstants;




/**
 *
 * @author teras
 */
public class SubStationAlpha extends AbstractTextSubFormat {
    
    private static final Pattern pat, testpat;
    
    private static final Pattern title, author, source, comments, styles;
    
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
    }
    
    protected Pattern getPattern() {
        return pat;
    }
    
    protected Pattern getTestPattern() {
        return testpat;
    }
    
    public boolean supportsFPS() { return false; }
    
    
    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5)+"0");
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9)+"0");
        SubEntry entry = new SubEntry(start, finish, "");
        entry.setStyle(subtitle_list.getStyleList().getStyleByName(m.group(10)));
        parseSubText(entry, m.group(16));
        
        // m.group(1) is still unusable
        // m.group(11-15) are still unusable
        return entry;
    }
    
    
    public String getExtension() {
        return "ssa";
    }
    
    public String getName() {
        return "SubStationAlpha";
    }
    
    
    private String timeformat(Time t) {
        String res = t.toString().substring(1).replace(',','.');
        res = res.substring(0, res.length()-1);
        return res;
    }
    
    
    protected void appendSubEntry(SubEntry sub, StringBuffer str){
        str.append("Dialogue: ");
        str.append(getLayer()).append(',');
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
        str.append(rebuildSubText(sub));
        str.append("\n");
    }
    
    
    
    protected String rebuildSubText(SubEntry entry) {
        TreeSet<SubEv> events = new TreeSet<SubEv>();
        AbstractStyleover over;
        StyleoverEvent ev;
        String tag;
        String data;
        entry.cleanupEvents();
        
        /* Put all events in a sorted list */
        if (entry.overstyle != null) {  // First assure that the overstyle list is not empty
            for (int i = 0 ; i < entry.overstyle.length ; i++) {    // Now iterate for every entry in the overstyle list
                over = entry.overstyle[i];  // Get this AbstractStyleover
                if (over != null) {         // And make sure that it exists
                    tag = getTagFromStyle(over.getStyleType());     // Find the tag we are going to use for this style. If it is unsupported null will be returned
                    if (tag!=null) {                                // OK, style is supported - go on
                        for (int j = 0 ; j < over.size() ; j++) {   // Iterate through all events in this AbstractStyleover list
                            ev = over.getVisibleEvent(j);           // Get the value data
                            if (ev.value instanceof Boolean) data = ((Boolean)ev.value)?"1":"0";
                            else if (ev.value instanceof Direction) data = String.valueOf(directionToInt((Direction)ev.value));
                            else if (ev.value instanceof AlphaColor) {
                                String alpha = (tag.charAt(1)=='c') ? "alpha" : tag.charAt(1)+"a";  // This is actually valid only for the Advanvced sub station, but we put it here too since it does no harm
                                String bgr = Integer.toHexString(reverseOrder(((AlphaColor)ev.value).getRGB() & 0xffffff));
                                String a = Integer.toHexString(fixAlpha(((AlphaColor)ev.value).getAlpha()));
                                bgr = "000000".substring(0, 6-bgr.length()) + bgr;
                                a = "00".substring(0, 2-a.length()) + a;
                                data = "&H"+bgr+"&\\"+alpha+"&H"+a+"&";
                            } else data = ev.value.toString();
                            events.add( new SubEv(tag+data, ev.position));
                        }
                    }
                }
            }
        }
        
        StringBuffer btxt = new StringBuffer(entry.getText());
        for (SubEv cevent : events) {
            if (btxt.charAt(cevent.start)!='{') btxt.insert(cevent.start, "{}");
            btxt.insert(cevent.start+1, cevent.value);
        }
        return btxt.toString().replace("\n", "\\N");
    }
    
    protected String getTagFromStyle(Object style) {
        if (style==StyleConstants.FontFamily) return "\\fn";
        else if (style==StyleConstants.FontSize) return "\\fs";
        else if (style==StyleConstants.Bold) return "\\b";
        else if (style==StyleConstants.Italic) return "\\i";
        else if (style==StyleConstants.Foreground) return "\\c";
        else if (style==StyleConstants.Alignment) return "\\a";
        else if (style.equals("unknown")) return "\\";
        return null;
    }
    
    
    protected void parseSubText(SubEntry entry, String txt) {
        
        StringBuffer sbuf = new StringBuffer(txt.replace("\\N", "\n"));
        Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(sbuf);
        ArrayList<SubEv> events = new ArrayList<SubEv>();
        SubEv s;
        int startoffset = 0 ; /* In order for not parsing the events when the text will be truncated, store the offset right here and substruct it on any new event */
        /* First parse the sub text and add all events */
        while (m.find()) {
            s = new SubEv(m.group(1), m.start(1)-1-startoffset);
            events.add(s);
            startoffset += s.value.length() + 2;
        }
        
        /* Fix the Subtitle text */
        for (SubEv se : events) {
            sbuf.delete(se.start, se.start + se.value.length()+2);
        }
        entry.setText(sbuf.toString());
        
        
        String tok; // Where to store the current event
        StringTokenizer tokens; // handler for the tokens of events
        AlphaColor ccol1 = null, ccol2 = null, ccol3 = null, ccol4 = null;  // Since color is split into alpha & BGR, we use this to combiine them
        // Go through all events - if we don't know what to do just store it back again
        for (SubEv se : events) {
            tokens = new StringTokenizer(se.value,  "\\");
            while (tokens.hasMoreElements()) {
                tok = tokens.nextToken();
                if (tok.startsWith("i")) entry.addOverStyle(ITALIC, tok.substring(1).equals("1"), se.start);
                else if (tok.startsWith("b")) entry.addOverStyle(BOLD, tok.substring(1).equals("1"), se.start);
                else if (tok.startsWith("u")) entry.addOverStyle(UNDERLINE, tok.substring(1).equals("1"), se.start);
                else if (tok.startsWith("s")) entry.addOverStyle(STRIKETHROUGH, tok.substring(1).equals("1"), se.start);
                else if (tok.startsWith("fn")) entry.addOverStyle(FONTNAME, tok.substring(2), se.start);
                else if (tok.startsWith("fs")) entry.addOverStyle(FONTSIZE, Integer.parseInt(tok.substring(2)), se.start);
                else if (tok.startsWith("fs")) entry.addOverStyle(FONTSIZE, Integer.parseInt(tok.substring(2)), se.start);
                else if (tok.startsWith("c")) ccol1 = setSubColor(ccol1, entry, PRIMARY, tok.substring(3, tok.length()-1), se.start);
                else if (tok.startsWith("1c")) ccol1 = setSubColor(ccol1, entry, PRIMARY, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("2c")) ccol2 = setSubColor(ccol2, entry, SECONDARY, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("3c")) ccol3 = setSubColor(ccol3, entry, OUTLINE, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("4c")) ccol4 = setSubColor(ccol4, entry, SHADOW, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("alpha")) ccol1 = setSubAlpha(ccol1, entry, PRIMARY, tok.substring(7, tok.length()-1), se.start);
                else if (tok.startsWith("1a")) ccol1 = setSubAlpha(ccol1, entry, PRIMARY, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("2a")) ccol2 = setSubAlpha(ccol2, entry, SECONDARY, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("3a")) ccol3 = setSubAlpha(ccol3, entry, OUTLINE, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("4a")) ccol4 = setSubAlpha(ccol4, entry, SHADOW, tok.substring(4, tok.length()-1), se.start);
                else if (tok.startsWith("a")) entry.setOverStyle(DIRECTION, intToDirection(Integer.parseInt(tok.substring(1))), se.start, se.start);
                else entry.addOverStyle(UNKNOWN, tok, se.start);
            }
        }
        entry.cleanupEvents();
    }
    
    
    protected AlphaColor setSubColor(AlphaColor col, SubEntry entry, SubStyle.Style style, String val, int start) {
        if (col==null) col=new AlphaColor(Color.WHITE, 255);
        AlphaColor c = new AlphaColor((col.getAlpha()<<24) | reverseOrder(Integer.parseInt(val,16)) );
        entry.addOverStyle(style, c, start);
        return c;
    }
    protected AlphaColor setSubAlpha(AlphaColor col, SubEntry entry, SubStyle.Style style, String val, int start) {
        if (col==null) col=new AlphaColor(Color.WHITE, 255);
        AlphaColor c = new AlphaColor( (fixAlpha(Integer.parseInt(val, 16)&0xff)<<24) | (col.getRGB()&0xffffff) );
        entry.addOverStyle(style, c, start);
        return c;
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

        header.append("PlayResX: ").append(media.getVideoFile().getWidth());
        header.append("\nPlayResY: ").append(media.getVideoFile().getHeight());
        
        header.append("\nPlayDepth: 0\nTimer: 100,0000\n");
        
        header.append("\n[V4 Styles");
        header.append(getExtraVersion());
        header.append("]\n");
        appendStyles(subs, header);
        
        header.append("\n[Events]\nFormat: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
    }
    
    protected String initLoader(String input) {
        input = super.initLoader(input);
        getStyles(input);
        updateAttributes(input, title, author, source, comments);
        return input;
    }
    
    protected String getExtraVersion() { return ""; }
    protected String getLayer() { return "Marked=0"; }
    
    protected int booleanToInt(Object b) {
        return (((Boolean)b).booleanValue())?-1:0;
    }
    protected int directionToInt(Direction dir) {
        switch (dir) {
            case TOP:
                return 6;
            case TOPRIGHT:
                return 7;
            case RIGHT:
                return 11;
            case BOTTOMRIGHT:
                return 3;
            case BOTTOMLEFT:
                return 1;
            case LEFT:
                return 9;
            case TOPLEFT:
                return 5;
            case CENTER:
                return 10;
        }
        /* By default return bottom */
        return 2;
    }
    
    
    protected boolean strToBoolean(String val) { return !val.equals("0"); }
    
    protected Direction intToDirection(int i) {
        switch (i) {
            case 6:
                return TOP;
            case 7:
                return TOPRIGHT;
            case 11:
                return RIGHT;
            case 3:
                return BOTTOMRIGHT;
            case 1:
                return BOTTOMLEFT;
            case 9:
                return LEFT;
            case 5:
                return TOPLEFT;
            case 10:
                return CENTER;
        }
        /* By default return bottom */
        return BOTTOM;
    }
    
    
    protected void appendStyles(Subtitles subs, StringBuffer header) {
        header.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding\n");
        for (SubStyle style : subs.getStyleList()) {
            header.append("Style: ");
            header.append(style.Name).append(',');
            header.append(style.get(FONTNAME)).append(',');
            header.append(style.get(FONTSIZE)).append(',');
            header.append(setReverse(style.get(PRIMARY), false)).append(',');
            header.append(setReverse(style.get(SECONDARY), false)).append(',');
            header.append(setReverse(style.get(OUTLINE), false)).append(',');
            header.append(setReverse(style.get(SHADOW), true)).append(',');
            header.append(booleanToInt(style.get(BOLD))).append(',');
            header.append(booleanToInt(style.get(ITALIC))).append(',');
            header.append((((Integer)style.get(BORDERSTYLE)).intValue()==0)?1:3).append(',');
            header.append(style.get(BORDERSIZE)).append(',');
            header.append(style.get(SHADOWSIZE)).append(',');
            header.append(directionToInt((Direction)style.get(DIRECTION))).append(',');
            header.append(style.get(LEFTMARGIN)).append(',');
            header.append(style.get(RIGHTMARGIN)).append(',');
            header.append(style.get(VERTICAL)).append(',');
            header.append(((AlphaColor)style.get(PRIMARY)).getAlpha()).append(',');
            header.append(0).append('\n');
        }
        
    }
    
    protected void getStyles(String input) {
        Matcher m = styles.matcher(input);
        SubStyleList list = subtitle_list.getStyleList();
        list.clearList();
        
        SubStyle st;
        AlphaColor pri;
        while (m.find()) {
            st = new SubStyle(m.group(1).trim());
            st.set(FONTNAME, m.group(2));
            st.set(FONTSIZE, new Integer(m.group(3)));
            st.set(PRIMARY, getReverse(m.group(4), m.group(17)));
            st.set(SECONDARY, getReverse(m.group(5), m.group(17)));
            st.set(OUTLINE, getReverse(m.group(6), m.group(17)));
            st.set(SHADOW, getReverse(m.group(7), null));
            st.set(BOLD, strToBoolean(m.group(8)));
            st.set(ITALIC, strToBoolean(m.group(9)));
            st.set(BORDERSTYLE, (m.group(10).equals("1"))? 0 : 1);
            st.set(BORDERSIZE, new Integer(m.group(11)));
            st.set(SHADOWSIZE, new Integer(m.group(12)));
            st.set(DIRECTION, intToDirection(Integer.parseInt(m.group(13))));
            st.set(LEFTMARGIN, new Integer(m.group(14)));
            st.set(RIGHTMARGIN, new Integer(m.group(15)));
            st.set(VERTICAL, new Integer(m.group(16)));
            
            if (st.Name.equals("Default")) {
                list.elementAt(0).setValues(st);
            } else {
                list.add(st);
            }
        }
    }
    
    protected int reverseOrder(int old) {
        return ((old&0xff0000)>>16) | (old&0xff00) | ((old&0xff)<<16);
    }
    
    /* If the Alpha channel is stored in the BGR, then the Alpha parameter should be NULL */
    protected AlphaColor getReverse(String revRGB, String Alpha ) {
        long lrgb = Long.parseLong(revRGB);
        int rgb = (int)lrgb & 0xffffff;
        int alpha = ((int)lrgb&0xff000000) >> 24;
        if (Alpha!=null) alpha = Integer.parseInt(Alpha);
        alpha = fixAlpha(alpha) << 24;
        return new AlphaColor( alpha | reverseOrder(rgb) );
    }
    protected int setReverse(Object acolor, boolean store_alpha) {
        int rgb = ((AlphaColor)acolor).getRGB() & 0xffffff;
        int alpha = store_alpha ? ( fixAlpha(((AlphaColor)acolor).getAlpha()) << 24 ) : 0;
        return ( alpha | reverseOrder(rgb));
    }
    
    private int fixAlpha(int alpha) { return 0xff - alpha; }
    
    
    class SubEv implements Comparable<SubEv> {
        String value;
        int start;
        
        public SubEv(String val, int start) {
            value = val;
            this.start = start;
        }
        
        public boolean equals(Comparable other) {
            return compareTo((SubEv)other) == 0;
        }
        
        /* Since we want ot add the events in reverse order, the sorting is done on reverse! */
        public int compareTo(SubEv other) {
            if (start < other.start) return 1;
            if (start > other.start) return -1;
            if (value.equals(other)) return 0;
            return -1;  // In all other occasions, means that it's a different object so just put it somewhere
        }
    }
    
}
